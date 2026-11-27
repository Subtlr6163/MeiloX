package com.ljyh.mei.ui.component.item

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ljyh.mei.R
import com.ljyh.mei.source.api.TrackRef
import com.ljyh.mei.ui.component.CopyNamePopover
import com.ljyh.mei.ui.component.LocalCopyNamePopoverHost
import com.ljyh.mei.ui.component.copyNameActions
import com.ljyh.mei.ui.component.copyNameGesture
import io.github.neoruaa.glasmio.ui.GlasmioListRow
import io.github.neoruaa.glasmio.ui.GlasmioListRowText

@Composable
internal fun GroupedSongRow(
    title: String,
    artist: String,
    leading: @Composable () -> Unit,
    onClick: () -> Unit,
    showTopSeparator: Boolean,
    subtitle: String? = artist,
    detail: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    trackRef: TrackRef? = null,
    hasAlternateVersion: Boolean = false,
) {
    val alternateVersion = trackAlternateVersionAction(trackRef, title, hasAlternateVersion)
    val hasHost = LocalCopyNamePopoverHost.current != null
    val subtitleContent: (@Composable () -> Unit)? = subtitle?.let { text ->
        @Composable {
            if (hasHost) {
                GlasmioListRowText(
                    text = text,
                    secondary = true,
                    modifier = Modifier.copyNameGesture(
                        name = artist,
                        actionLabel = R.string.artist_action_copy_name,
                        accessibilityOnParent = true,
                    ),
                )
            } else {
                CopyNamePopover(
                    name = artist,
                    actionLabel = R.string.artist_action_copy_name,
                    accessibilityOnParent = true,
                ) { gesture, _ ->
                    GlasmioListRowText(text = text, secondary = true, modifier = gesture)
                }
            }
        }
    }
    GlasmioListRow(
        titleContent = {
            if (hasHost) {
                GlasmioListRowText(
                    text = title,
                    modifier = Modifier.copyNameGesture(
                        name = title,
                        actionLabel = R.string.track_action_copy_name,
                        accessibilityOnParent = true,
                    ),
                )
            } else {
                CopyNamePopover(
                    name = title,
                    actionLabel = R.string.track_action_copy_name,
                    accessibilityOnParent = true,
                ) { gesture, _ ->
                    GlasmioListRowText(text = title, modifier = gesture)
                }
            }
        },
        modifier = Modifier.copyNameActions(
            trackName = title,
            artistName = artist,
            enabled = true,
        ),
        subtitleContent = if (alternateVersion == null) subtitleContent else {
            {
                AlternateVersionSubtitle(showBadge = true, onBadgeClick = alternateVersion.onBadgeClick) {
                    subtitleContent?.invoke()
                }
            }
        },
        detail = detail,
        leading = leading,
        onClick = alternateVersion?.onRowClick ?: onClick,
        trailing = trailing,
        showTopSeparator = showTopSeparator,
    )
}
