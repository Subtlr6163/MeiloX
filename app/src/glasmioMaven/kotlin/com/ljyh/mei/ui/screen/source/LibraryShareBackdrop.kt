package com.ljyh.mei.ui.screen.source

import androidx.compose.runtime.Composable
import io.github.neoruaa.glasmio.glass.render.Backdrop

internal const val SupportsDetachedLibraryShareBackdrop = false

// The published library uses the live backdrop.
@Suppress("UNUSED_PARAMETER")
@Composable
internal fun rememberLibraryShareBackdrop(
    backdrop: Backdrop,
    enabled: () -> Boolean,
): Backdrop = backdrop
