package com.ljyh.mei.ui.component.player.component

import android.content.pm.ActivityInfo
import android.graphics.Paint
import android.graphics.RuntimeXfermode
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.LayerOutsets
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.invalidatePlacement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density

@Composable
private fun playerUsesHdrWindow(): Boolean =
    LocalActivity.current?.window?.colorMode == ActivityInfo.COLOR_MODE_HDR

@Composable
internal fun usesLegacyPlayerBlendClamp(): Boolean =
    Build.VERSION.SDK_INT in 33..35 && playerUsesHdrWindow()

/** Keep additive player colors within SDR white, even on a floating-point HDR target. */
internal fun Modifier.playerAdditiveLayer(
    opacity: (() -> Float)? = null,
    translation: (() -> Offset)? = null,
    layerOutsets: (Density.(Size) -> LayerOutsets)? = null,
): Modifier = this
    .then(PlayerAdditiveTransformElement(opacity, translation, layerOutsets))
    .then(PlayerAdditiveDrawElement(layerOutsets))

private fun CompositionLocalConsumerModifierNode.usesApi36HdrWindow(): Boolean =
    Build.VERSION.SDK_INT >= 36 &&
        currentValueOf(LocalActivity)?.window?.colorMode == ActivityInfo.COLOR_MODE_HDR

private data class PlayerAdditiveTransformElement(
    val opacity: (() -> Float)?,
    val translation: (() -> Offset)?,
    val layerOutsets: (Density.(Size) -> LayerOutsets)?,
) : ModifierNodeElement<PlayerAdditiveTransformNode>() {
    override fun create() = PlayerAdditiveTransformNode(opacity, translation, layerOutsets)

    override fun update(node: PlayerAdditiveTransformNode) {
        node.opacity = opacity
        node.translation = translation
        node.layerOutsets = layerOutsets
        node.invalidatePlacement()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "playerAdditiveLayerTransform"
    }
}

private class PlayerAdditiveTransformNode(
    var opacity: (() -> Float)?,
    var translation: (() -> Offset)?,
    var layerOutsets: (Density.(Size) -> LayerOutsets)?,
) : Modifier.Node(), LayoutModifierNode, CompositionLocalConsumerModifierNode {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        val api36Hdr = usesApi36HdrWindow()
        return layout(placeable.width, placeable.height) {
            if (api36Hdr) {
                if (translation != null || opacity != null) {
                    // Apply translation and animated opacity on a hardware RenderNode outside the
                    // saveLayer so opacity/translation frames update layer properties without
                    // re-recording descendant display lists.
                    placeable.placeWithLayer(0, 0) {
                        translation?.invoke()?.let { offset ->
                            translationX = offset.x
                            translationY = offset.y
                        }
                        opacity?.let { provider ->
                            alpha = provider().coerceIn(0f, 1f)
                            compositingStrategy = CompositingStrategy.ModulateAlpha
                        }
                    }
                } else {
                    placeable.place(0, 0)
                }
            } else {
                placeable.placeWithLayer(0, 0) {
                    translation?.invoke()?.let { offset ->
                        translationX = offset.x
                        translationY = offset.y
                    }
                    alpha = opacity?.invoke() ?: 1f
                    outsets = layerOutsets?.invoke(this, size) ?: LayerOutsets.Zero
                    blendMode = BlendMode.Plus
                    compositingStrategy = CompositingStrategy.Offscreen
                }
            }
        }
    }
}

private data class PlayerAdditiveDrawElement(
    val layerOutsets: (Density.(Size) -> LayerOutsets)?,
) : ModifierNodeElement<PlayerAdditiveDrawNode>() {
    override fun create() = PlayerAdditiveDrawNode(layerOutsets)

    override fun update(node: PlayerAdditiveDrawNode) {
        if (node.layerOutsets !== layerOutsets) {
            node.layerOutsets = layerOutsets
            node.invalidateDraw()
        }
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "playerAdditiveLayerDraw"
    }
}

private class PlayerAdditiveDrawNode(
    var layerOutsets: (Density.(Size) -> LayerOutsets)?,
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {
    private var cachedPaint: Paint? = null

    override fun ContentDrawScope.draw() {
        if (!usesApi36HdrWindow()) {
            drawContent()
            return
        }
        val paint = cachedPaint ?: Paint().apply {
            xfermode = PlayerBlendApi36.additiveXfermode
        }.also { cachedPaint = it }
        val outsets = layerOutsets?.invoke(this, size) ?: LayerOutsets.Zero
        val left = -outsets.left.toPx()
        val top = -outsets.top.toPx()
        val right = size.width + outsets.right.toPx()
        val bottom = size.height + outsets.bottom.toPx()
        val canvas = drawContext.canvas.nativeCanvas
        val checkpoint = canvas.save()
        try {
            canvas.clipRect(left, top, right, bottom)
            canvas.saveLayer(left, top, right, bottom, paint)
            drawContent()
        } finally {
            canvas.restoreToCount(checkpoint)
        }
    }
}

@RequiresApi(36)
internal object PlayerBlendApi36 {
    val additiveXfermode = RuntimeXfermode(
        """
        half4 main(half4 source, half4 destination) {
            half alpha = min(source.a + destination.a, 1.0);
            // Android can scale SDR white below 1 in a normalized HDR destination.
            half3 white = fromLinearSrgb(half3(1.0));
            half3 ceiling = max(destination.rgb, white * alpha);
            return half4(min(source.rgb + destination.rgb, ceiling), alpha);
        }
        """.trimIndent(),
    )
}
