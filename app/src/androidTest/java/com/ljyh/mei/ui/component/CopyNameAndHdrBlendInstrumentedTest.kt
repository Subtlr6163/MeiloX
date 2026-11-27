package com.ljyh.mei.ui.component

import android.content.ClipboardManager
import android.content.Context
import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ljyh.mei.R
import com.ljyh.mei.ui.component.item.GroupedSongRow
import com.ljyh.mei.ui.component.item.MediaRowName
import com.ljyh.mei.ui.component.player.component.playerAdditiveLayer
import io.github.neoruaa.glasmio.core.GlassAppearance
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.glass.LocalBlurBackdrop
import io.github.neoruaa.glasmio.glass.LocalGlassBackdrop
import io.github.neoruaa.glasmio.glass.render.backdrops.rememberCanvasBackdrop
import io.github.neoruaa.glasmio.glass.render.configureGlassDynamicRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CopyNameAndHdrBlendInstrumentedTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun copyNameHostOpensOnLongPressCopiesToClipboardAndDismissesWhenDisabled() {
        var enabled by mutableStateOf(true)
        var rowClicks = 0
        compose.setContent {
            val backdrop = rememberCanvasBackdrop(Color.Black)
            GlasmioTheme(appearance = GlassAppearance(reduceTransparency = true)) {
                CompositionLocalProvider(
                    LocalGlassBackdrop provides backdrop,
                    LocalBlurBackdrop provides backdrop,
                ) {
                    CopyNamePopoverHost(Modifier.fillMaxSize()) {
                        Column(Modifier.fillMaxWidth().padding(24.dp)) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .testTag("song-row")
                                    .copyNameActions(
                                        trackName = "Test Song Title",
                                        artistName = "Test Artist",
                                        enabled = enabled,
                                    )
                                    .clickable { rowClicks++ },
                            ) {
                                MediaRowName(
                                    text = "Test Song Title",
                                    style = androidx.compose.material3.LocalTextStyle.current,
                                    color = Color.White,
                                    copyAction = R.string.track_action_copy_name.takeIf { enabled },
                                )
                            }
                            GroupedSongRow(
                                title = "Grouped Song",
                                artist = "Grouped Artist",
                                leading = { Text("1") },
                                onClick = { rowClicks++ },
                                showTopSeparator = false,
                            )
                        }
                    }
                }
            }
        }

        // Ordinary tap preserves row click without opening the copy popover.
        compose.onNodeWithText("Test Song Title", useUnmergedTree = true).performClick()
        compose.runOnIdle { assertEquals(1, rowClicks) }
        val copyLabel = compose.activity.getString(R.string.name_copy_action)
        compose.onAllNodesWithText(copyLabel).assertCountEquals(0)

        // Accessibility customActions are exposed on the parent row without vararg allocations.
        val rowNode = compose.onNodeWithTag("song-row").fetchSemanticsNode()
        val actions = rowNode.config[SemanticsActions.CustomActions]
        assertEquals(2, actions.size)

        // Long-press on the Text itself opens the shared CopyNamePopoverHost without firing row click.
        compose.onNodeWithText("Test Song Title", useUnmergedTree = true).performTouchInput { longClick() }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText(copyLabel).fetchSemanticsNodes().size == 1
        }
        compose.runOnIdle { assertEquals(1, rowClicks) }

        // Disabling the trigger automatically dismisses the active popover.
        compose.runOnIdle { enabled = false }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText(copyLabel).fetchSemanticsNodes().isEmpty()
        }

        // GroupedSongRow uses GlasmioListRowText and copies to clipboard on action click.
        compose.onNodeWithText("Grouped Song", useUnmergedTree = true).performTouchInput { longClick() }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText(copyLabel).fetchSemanticsNodes().size == 1
        }
        compose.onNodeWithText(copyLabel).performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodes(isPopup()).fetchSemanticsNodes().isEmpty()
        }
        compose.runOnIdle {
            val cm = compose.activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipText = cm.primaryClip?.getItemAt(0)?.text?.toString()
            if (clipText != null) {
                assertEquals("Grouped Song", clipText)
            }
        }
    }

    @Test
    fun hdrPlayerAdditiveLayerModulatesOpacityAndCapsBrightnessOnHdrWindow() {
        compose.runOnUiThread {
            configureGlassDynamicRange(compose.activity.window, hdrEnabled = true)
        }
        var layerOpacity by mutableFloatStateOf(0f)
        val background = Color(0.20f, 0.30f, 0.40f)
        compose.setContent {
            GlasmioTheme {
                Column(Modifier.testTag("hdr-additive-scene")) {
                    Box(Modifier.width(96.dp).height(48.dp).background(background).testTag("hdr-ref-bg"))
                    Box(Modifier.width(96.dp).height(48.dp).background(Color.White).testTag("hdr-ref-white"))
                    Box(
                        Modifier
                            .width(96.dp)
                            .height(48.dp)
                            .background(background)
                            .testTag("hdr-additive-target"),
                    ) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .playerAdditiveLayer(opacity = { layerOpacity })
                                .background(Color.White.copy(alpha = 0.5f)),
                        )
                    }
                    Box(
                        Modifier
                            .width(96.dp)
                            .height(48.dp)
                            .background(Color(0.8f, 0.8f, 0.8f))
                            .testTag("hdr-additive-capped"),
                    ) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .playerAdditiveLayer(opacity = { 1f })
                                .background(Color.White),
                        )
                    }
                }
            }
        }

        val isHdr = compose.runOnUiThread {
            compose.activity.window.colorMode == ActivityInfo.COLOR_MODE_HDR
        }
        assertTrue("Expected test device window to enter COLOR_MODE_HDR", isHdr)

        fun sampleCenter(tag: String): Color {
            val pixels = compose.onNodeWithTag(tag).captureToImage().toPixelMap()
            return pixels[pixels.width / 2, pixels.height / 2]
        }

        compose.runOnIdle { layerOpacity = 0f }
        val refBg = sampleCenter("hdr-ref-bg")
        val refWhite = sampleCenter("hdr-ref-white")
        val atZero = sampleCenter("hdr-additive-target")
        assertEquals("Opacity 0 must match background red", refBg.red, atZero.red, 0.02f)
        assertEquals("Opacity 0 must match background green", refBg.green, atZero.green, 0.02f)
        assertEquals("Opacity 0 must match background blue", refBg.blue, atZero.blue, 0.02f)

        compose.runOnIdle { layerOpacity = 0.5f }
        val atHalf = sampleCenter("hdr-additive-target")

        compose.runOnIdle { layerOpacity = 1f }
        val atFull = sampleCenter("hdr-additive-target")
        val capped = sampleCenter("hdr-additive-capped")

        assertTrue(
            "ModulateAlpha must scale additive contribution monotonically: zero=${atZero.red}, half=${atHalf.red}, full=${atFull.red}",
            atHalf.red > atZero.red + 0.05f && atFull.red > atHalf.red + 0.05f,
        )
        assertEquals("Additive white over bright background must cap at SDR white red", refWhite.red, capped.red, 0.02f)
        assertEquals("Additive white over bright background must cap at SDR white green", refWhite.green, capped.green, 0.02f)
        assertEquals("Additive white over bright background must cap at SDR white blue", refWhite.blue, capped.blue, 0.02f)
    }
}
