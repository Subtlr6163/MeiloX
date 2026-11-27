package com.ljyh.mei.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isFinite
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ljyh.mei.R
import com.ljyh.mei.utils.setClipboard
import io.github.neoruaa.glasmio.core.LocalGlassColors
import io.github.neoruaa.glasmio.core.LocalGlasmioTextStyles
import io.github.neoruaa.glasmio.glass.GlasmioMenuAnchorState
import io.github.neoruaa.glasmio.ui.GlasmioPopover
import kotlinx.coroutines.delay

@Stable
internal class CopyNamePopoverController {
    internal class Request(
        val ownerToken: Any,
        val name: String,
        @param:StringRes val actionLabelRes: Int,
        val anchorBounds: () -> Rect?,
    )

    var activeRequest by mutableStateOf<Request?>(null)
        private set

    fun show(
        ownerToken: Any,
        name: String,
        @StringRes actionLabelRes: Int,
        anchorBounds: () -> Rect?,
    ) {
        if (name.isBlank()) return
        activeRequest = Request(ownerToken, name, actionLabelRes, anchorBounds)
    }

    fun dismiss(ownerToken: Any? = null) {
        val current = activeRequest ?: return
        if (ownerToken == null || current.ownerToken === ownerToken) {
            activeRequest = null
        }
    }

    fun isVisibleFor(ownerToken: Any): Boolean = activeRequest?.ownerToken === ownerToken
}

internal val LocalCopyNamePopoverHost = staticCompositionLocalOf<CopyNamePopoverController?> { null }

/**
 * Provides a single shared [GlasmioPopover] overlay for all descendant copy-name triggers,
 * avoiding per-row popup/transition composition overhead in scrolling song lists.
 */
@Composable
internal fun CopyNamePopoverHost(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val controller = remember { CopyNamePopoverController() }
    CompositionLocalProvider(LocalCopyNamePopoverHost provides controller) {
        Box(modifier) {
            content()
            CopyNamePopoverOverlay(controller)
        }
    }
}

@Composable
private fun CopyNamePopoverOverlay(controller: CopyNamePopoverController) {
    val active = controller.activeRequest
    var retained by remember { mutableStateOf<CopyNamePopoverController.Request?>(null) }
    if (active != null) retained = active
    LaunchedEffect(active) {
        if (active == null && retained != null) {
            // Keep the last request only while the exit spring finishes, then drop any node reference.
            delay(600)
            retained = null
        }
    }
    val display = active ?: retained ?: return
    val context = LocalContext.current
    val accent = LocalGlassColors.current.accent
    val label = stringResource(display.actionLabelRes)
    val copyLabel = stringResource(R.string.name_copy_action)
    val confirmation = stringResource(R.string.name_copied)
    GlasmioPopover(
        visible = active != null,
        onDismissRequest = { controller.dismiss() },
        preferBelowAnchor = true,
        adaptToSheet = false,
        title = label,
        anchorBoundsProvider = display.anchorBounds,
        anchor = {},
    ) {
        CopyNamePopoverBody(
            name = display.name,
            label = label,
            copyLabel = copyLabel,
            accent = accent,
            onCopy = {
                controller.dismiss()
                setClipboard(context, display.name, label, confirmation)
            },
        )
    }
}

@Composable
private fun CopyNamePopoverBody(
    name: String,
    label: String,
    copyLabel: String,
    accent: Color,
    onCopy: () -> Unit,
) {
    val bodyStyle = LocalGlasmioTextStyles.current.body
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            modifier = Modifier.weight(1f, fill = false),
            style = bodyStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        VerticalDivider(
            modifier = Modifier.height(14.dp),
            color = LocalContentColor.current.copy(alpha = 0.38f),
        )
        Text(
            text = copyLabel,
            color = accent,
            style = bodyStyle,
            modifier = Modifier.clickable(role = Role.Button, onClickLabel = label, onClick = onCopy),
        )
    }
}

@Composable
internal fun CopyNamePopover(
    name: String,
    @StringRes actionLabel: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accessibilityOnParent: Boolean = false,
    textAnchor: CopyNameAnchor? = null,
    openOnClick: Boolean = false,
    observeVisibility: Boolean = false,
    sharedMenuAnchor: GlasmioMenuAnchorState? = null,
    content: @Composable (gesture: Modifier, popoverVisible: Boolean) -> Unit,
) {
    val host = LocalCopyNamePopoverHost.current
    if (host != null) {
        val ownerToken = remember { Any() }
        val gesture = Modifier.copyNameGesture(
            name = name,
            actionLabel = actionLabel,
            enabled = enabled,
            accessibilityOnParent = accessibilityOnParent,
            textAnchor = textAnchor,
            openOnClick = openOnClick,
            sharedMenuAnchor = sharedMenuAnchor,
            ownerToken = ownerToken,
        )
        val popoverVisible = if (observeVisibility) host.isVisibleFor(ownerToken) else false
        if (modifier === Modifier) {
            content(gesture, popoverVisible)
        } else {
            Box(modifier, propagateMinConstraints = true) {
                content(gesture, popoverVisible)
            }
        }
        return
    }

    // Standalone fallback when rendered outside a CopyNamePopoverHost (e.g. isolated tests/previews).
    var visible by remember { mutableStateOf(false) }
    var capturedName by remember { mutableStateOf(name) }
    var capturedBoundsProvider by remember { mutableStateOf<(() -> Rect?)?>(null) }
    LaunchedEffect(enabled) {
        if (!enabled) visible = false
    }
    val gesture = Modifier.copyNameGesture(
        name = name,
        actionLabel = actionLabel,
        enabled = enabled,
        accessibilityOnParent = accessibilityOnParent,
        textAnchor = textAnchor,
        openOnClick = openOnClick,
        sharedMenuAnchor = sharedMenuAnchor,
        onOpenFallback = { resolvedName, boundsProvider ->
            capturedName = resolvedName
            capturedBoundsProvider = boundsProvider
            visible = true
        },
    )
    val showPopup = visible && enabled
    GlasmioPopover(
        visible = showPopup,
        onDismissRequest = { visible = false },
        preferBelowAnchor = true,
        adaptToSheet = false,
        title = if (showPopup) stringResource(actionLabel) else "",
        anchorBoundsProvider = capturedBoundsProvider,
        modifier = modifier,
        anchor = { content(gesture, showPopup) },
    ) {
        val context = LocalContext.current
        val accent = LocalGlassColors.current.accent
        val label = stringResource(actionLabel)
        val copyLabel = stringResource(R.string.name_copy_action)
        val confirmation = stringResource(R.string.name_copied)
        CopyNamePopoverBody(
            name = capturedName,
            label = label,
            copyLabel = copyLabel,
            accent = accent,
            onCopy = {
                visible = false
                setClipboard(context, capturedName, label, confirmation)
            },
        )
    }
}

/**
 * Lightweight long-press (or click) trigger that opens the nearest [CopyNamePopoverHost]
 * without attaching `onGloballyPositioned` callbacks during list scrolling or composing a
 * per-item `GlasmioPopover`.
 */
internal fun Modifier.copyNameGesture(
    name: String,
    @StringRes actionLabel: Int,
    enabled: Boolean = true,
    accessibilityOnParent: Boolean = false,
    textAnchor: CopyNameAnchor? = null,
    openOnClick: Boolean = false,
    sharedMenuAnchor: GlasmioMenuAnchorState? = null,
    ownerToken: Any? = null,
    onOpenFallback: ((String, () -> Rect?) -> Unit)? = null,
): Modifier = if (!enabled || name.isBlank()) {
    this
} else {
    this then CopyNameGestureElement(
        name = name,
        actionLabel = actionLabel,
        accessibilityOnParent = accessibilityOnParent,
        textAnchor = textAnchor,
        openOnClick = openOnClick,
        sharedMenuAnchor = sharedMenuAnchor,
        ownerToken = ownerToken,
        onOpenFallback = onOpenFallback,
    )
}

private data class CopyNameGestureElement(
    val name: String,
    @param:StringRes val actionLabel: Int,
    val accessibilityOnParent: Boolean,
    val textAnchor: CopyNameAnchor?,
    val openOnClick: Boolean,
    val sharedMenuAnchor: GlasmioMenuAnchorState?,
    val ownerToken: Any?,
    val onOpenFallback: ((String, () -> Rect?) -> Unit)?,
) : ModifierNodeElement<CopyNameGestureNode>() {
    override fun create() = CopyNameGestureNode(
        name = name,
        actionLabel = actionLabel,
        accessibilityOnParent = accessibilityOnParent,
        textAnchor = textAnchor,
        openOnClick = openOnClick,
        sharedMenuAnchor = sharedMenuAnchor,
        ownerToken = ownerToken,
        onOpenFallback = onOpenFallback,
    )

    override fun update(node: CopyNameGestureNode) {
        node.update(
            name = name,
            actionLabel = actionLabel,
            accessibilityOnParent = accessibilityOnParent,
            textAnchor = textAnchor,
            openOnClick = openOnClick,
            sharedMenuAnchor = sharedMenuAnchor,
            ownerToken = ownerToken,
            onOpenFallback = onOpenFallback,
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "copyNameGesture"
    }
}

private class CopyNameGestureNode(
    var name: String,
    @param:StringRes var actionLabel: Int,
    var accessibilityOnParent: Boolean,
    var textAnchor: CopyNameAnchor?,
    var openOnClick: Boolean,
    var sharedMenuAnchor: GlasmioMenuAnchorState?,
    var ownerToken: Any?,
    var onOpenFallback: ((String, () -> Rect?) -> Unit)?,
) : DelegatingNode(), CompositionLocalConsumerModifierNode, SemanticsModifierNode {

    private val defaultToken = Any()
    private val effectiveToken: Any get() = ownerToken ?: defaultToken
    private var attachedHost: CopyNamePopoverController? = null

    private val pointerNode = delegate(
        SuspendingPointerInputModifierNode {
            awaitEachGesture {
                if (openOnClick) {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val coords = requireLayoutCoordinates().takeIf { it.isAttached } ?: return@awaitEachGesture
                    val origin = coords.localToScreen(down.position)
                    if (!origin.isFinite) return@awaitEachGesture
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (pointer.isConsumed || event.changes.any { it.id != down.id && it.pressed }) break
                        val currentPos = requireLayoutCoordinates().takeIf { it.isAttached }
                            ?.localToScreen(pointer.position)
                        if (currentPos == null || !currentPos.isFinite ||
                            (currentPos - origin).getDistance() > viewConfiguration.touchSlop
                        ) break
                        if (!pointer.pressed) {
                            pointer.consume()
                            triggerOpen(performHaptic = false)
                            break
                        }
                    }
                    return@awaitEachGesture
                }

                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                if (down.type != PointerType.Touch && down.type != PointerType.Stylus) return@awaitEachGesture
                val initialCoords = requireLayoutCoordinates().takeIf { it.isAttached } ?: return@awaitEachGesture
                val origin = initialCoords.localToScreen(down.position)
                if (!origin.isFinite) return@awaitEachGesture

                val selection = sharedMenuAnchor?.dragSelection
                val dragToken = if (selection != null) Any() else null
                if (selection != null && dragToken != null && !selection.acquire(dragToken, origin)) {
                    down.consume()
                    return@awaitEachGesture
                }

                try {
                    val cancelled = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                        while (true) {
                            val changes = awaitPointerEvent(PointerEventPass.Final).changes
                            val pointer = changes.firstOrNull { it.id == down.id } ?: break
                            if (!pointer.pressed || changes.any { it.id != down.id && it.pressed }) break
                            if (pointer.previousPressed && pointer.isConsumed) break
                            val point = requireLayoutCoordinates().takeIf { it.isAttached }
                                ?.localToScreen(pointer.position)?.takeIf { it.isFinite } ?: break
                            if ((point - origin).getDistance() > viewConfiguration.touchSlop) break
                        }
                        true
                    } ?: false
                    if (cancelled) return@awaitEachGesture

                    triggerOpen(performHaptic = true)
                    while (true) {
                        val changes = awaitPointerEvent(PointerEventPass.Initial).changes
                        val pointer = changes.firstOrNull { it.id == down.id } ?: break
                        if (pointer.isConsumed || changes.any { it.id != down.id && it.pressed }) break
                        pointer.consume()
                        if (!pointer.pressed) break
                    }
                } finally {
                    if (selection != null && dragToken != null) {
                        selection.finish(dragToken)
                    }
                }
            }
        },
    )

    private fun readCurrentBounds(): Rect? {
        textAnchor?.boundsInWindow()?.let { return it }
        if (!isAttached) return null
        val coords = requireLayoutCoordinates().takeIf { it.isAttached } ?: return null
        return coords.boundsInWindow().takeUnless { it.isEmpty }
    }

    private fun triggerOpen(performHaptic: Boolean) {
        if (!isAttached || name.isBlank()) return
        if (performHaptic) {
            currentValueOf(LocalHapticFeedback).performHapticFeedback(HapticFeedbackType.LongPress)
        }
        val snapshotBounds = readCurrentBounds()
        val boundsProvider: () -> Rect? = {
            readCurrentBounds() ?: snapshotBounds
        }
        val host = currentValueOf(LocalCopyNamePopoverHost)
        if (host != null) {
            attachedHost = host
            host.show(
                ownerToken = effectiveToken,
                name = name,
                actionLabelRes = actionLabel,
                anchorBounds = boundsProvider,
            )
        } else {
            onOpenFallback?.invoke(name, boundsProvider)
        }
    }

    fun update(
        name: String,
        @StringRes actionLabel: Int,
        accessibilityOnParent: Boolean,
        textAnchor: CopyNameAnchor?,
        openOnClick: Boolean,
        sharedMenuAnchor: GlasmioMenuAnchorState?,
        ownerToken: Any?,
        onOpenFallback: ((String, () -> Rect?) -> Unit)?,
    ) {
        val semanticsChanged = this.actionLabel != actionLabel ||
            this.accessibilityOnParent != accessibilityOnParent ||
            this.openOnClick != openOnClick
        val modeChanged = this.openOnClick != openOnClick
        this.name = name
        this.actionLabel = actionLabel
        this.accessibilityOnParent = accessibilityOnParent
        this.textAnchor = textAnchor
        this.openOnClick = openOnClick
        this.sharedMenuAnchor = sharedMenuAnchor
        this.ownerToken = ownerToken
        this.onOpenFallback = onOpenFallback
        if (modeChanged) pointerNode.resetPointerInputHandler()
        if (semanticsChanged) invalidateSemantics()
    }

    override fun onDetach() {
        attachedHost?.dismiss(effectiveToken)
        attachedHost = null
    }

    override val shouldMergeDescendantSemantics: Boolean
        get() = !accessibilityOnParent && !openOnClick

    override fun SemanticsPropertyReceiver.applySemantics() {
        if (!isAttached) return
        val label = currentValueOf(LocalContext).getString(actionLabel)
        if (openOnClick) {
            onClick(label = label) {
                triggerOpen(performHaptic = false)
                true
            }
        } else if (!accessibilityOnParent) {
            onLongClick(label = label) {
                triggerOpen(performHaptic = false)
                true
            }
        }
    }
}

internal class CopyNameAnchor {
    private var coordinates: LayoutCoordinates? = null
    private var lineBounds: Rect? = null

    val modifier = Modifier.onGloballyPositioned { coordinates = it }

    fun updateLayout(layout: TextLayoutResult) {
        if (layout.lineCount <= 0) {
            lineBounds = null
            return
        }
        val left = layout.getLineLeft(0)
        val right = layout.getLineRight(0)
        val height = layout.size.height.toFloat()
        lineBounds = if (left.isFinite() && right.isFinite() && height.isFinite() && right > left && height > 0f) {
            Rect(left, 0f, right, height)
        } else {
            null
        }
    }

    fun boundsInWindow(): Rect? {
        val positioned = coordinates?.takeIf { it.isAttached } ?: return null
        val visibleBounds = positioned.boundsInWindow()
        val line = lineBounds ?: return visibleBounds.takeUnless { it.isEmpty }
        val topLeft = positioned.localToWindow(Offset(line.left, line.top))
        val bottomRight = positioned.localToWindow(Offset(line.right, line.bottom))
        if (!topLeft.isFinite || !bottomRight.isFinite) {
            return visibleBounds.takeUnless { it.isEmpty }
        }
        val textBounds = Rect(
            minOf(topLeft.x, bottomRight.x),
            minOf(topLeft.y, bottomRight.y),
            maxOf(topLeft.x, bottomRight.x),
            maxOf(topLeft.y, bottomRight.y),
        )
        return textBounds.intersect(visibleBounds).takeUnless { it.isEmpty }
    }
}

/**
 * Exposes copy-name accessibility actions on a clickable row without vararg allocations or
 * eager `stringResource` reads during scrolling.
 */
internal fun Modifier.copyNameActions(
    trackName: String? = null,
    artistName: String? = null,
    albumName: String? = null,
    enabled: Boolean = true,
): Modifier = if (
    !enabled || (trackName.isNullOrBlank() && artistName.isNullOrBlank() && albumName.isNullOrBlank())
) {
    this
} else {
    this then CopyNameActionsElement(trackName, artistName, albumName)
}

private data class CopyNameActionsElement(
    val trackName: String?,
    val artistName: String?,
    val albumName: String?,
) : ModifierNodeElement<CopyNameActionsNode>() {
    override fun create() = CopyNameActionsNode(trackName, artistName, albumName)

    override fun update(node: CopyNameActionsNode) {
        node.update(trackName, artistName, albumName)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "copyNameActions"
    }
}

private class CopyNameActionsNode(
    private var trackName: String?,
    private var artistName: String?,
    private var albumName: String?,
) : Modifier.Node(), SemanticsModifierNode, CompositionLocalConsumerModifierNode {
    fun update(trackName: String?, artistName: String?, albumName: String?) {
        if (this.trackName == trackName && this.artistName == artistName && this.albumName == albumName) return
        this.trackName = trackName
        this.artistName = artistName
        this.albumName = albumName
        invalidateSemantics()
    }

    override fun SemanticsPropertyReceiver.applySemantics() {
        if (!isAttached) return
        val context = currentValueOf(LocalContext)
        val actions = ArrayList<CustomAccessibilityAction>(2)
        fun addAction(@StringRes labelRes: Int, value: String?) {
            val trimmed = value?.takeIf { it.isNotBlank() } ?: return
            val label = context.getString(labelRes)
            actions += CustomAccessibilityAction(label) {
                setClipboard(context, trimmed, label, context.getString(R.string.name_copied))
                true
            }
        }
        addAction(R.string.track_action_copy_name, trackName)
        addAction(R.string.artist_action_copy_name, artistName)
        addAction(R.string.album_action_copy_name, albumName)
        if (actions.isNotEmpty()) {
            customActions = actions
        }
    }
}
