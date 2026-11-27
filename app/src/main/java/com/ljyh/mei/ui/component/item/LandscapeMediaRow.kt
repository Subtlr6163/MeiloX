package com.ljyh.mei.ui.component.item

import com.ljyh.mei.ui.component.playlist.songListDividerColor
import androidx.annotation.StringRes
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.height
import com.ljyh.mei.constants.songListCoverSize
import com.ljyh.mei.constants.songListCoverCornerRadius
import com.ljyh.mei.ui.screen.playlist.component.DetailTrackDivider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import com.kyant.capsule.ContinuousRoundedRectangle
import com.ljyh.mei.R
import com.ljyh.mei.source.api.TrackRef
import com.ljyh.mei.ui.component.CopyNamePopover
import com.ljyh.mei.ui.component.LocalCopyNamePopoverHost
import com.ljyh.mei.ui.component.copyNameActions
import com.ljyh.mei.ui.component.copyNameGesture
import com.ljyh.mei.ui.component.playlist.FavoriteTrackStar
import com.ljyh.mei.ui.component.playlist.PlayingImageView
import com.ljyh.mei.ui.component.utils.rememberDeviceInfo
import io.github.neoruaa.glasmio.core.LocalGlassColors
import io.github.neoruaa.glasmio.glass.GlasmioMenuAnchorState
import io.github.neoruaa.glasmio.icons.SfIcon

private val PortraitSongCoverShape = ContinuousRoundedRectangle(songListCoverCornerRadius(false))
private val LandscapeSongCoverShape = ContinuousRoundedRectangle(songListCoverCornerRadius(true))

/** Shared landscape catalog columns; the album column follows the available viewport. */
@Composable
internal fun LandscapeMediaRow(
    image: String,
    title: String,
    artist: String,
    detail: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    album: String? = null,
    suffix: String? = null,
    suffixColor: Color? = null,
    favorite: Boolean = false,
    showDivider: Boolean = true,
    onMore: ((Rect) -> Unit)? = null,
    navigationArrow: Boolean = false,
    footer: (@Composable () -> Unit)? = null,
    landscape: Boolean = rememberDeviceInfo().isLandscape,
    isCurrentTrack: Boolean = false,
    showPressIndication: Boolean = false,
    fullWidthTrackPress: Boolean = false,
    menuAnchorState: GlasmioMenuAnchorState? = null,
    trackRef: TrackRef? = null,
    copyNamesOnLongPress: Boolean = false,
    hasAlternateVersion: Boolean = false,
) {
    val colors = LocalGlassColors.current
    if (!landscape) {
        PortraitMediaRow(
            image = image,
            title = title,
            artist = artist,
            detail = detail,
            onClick = onClick,
            modifier = modifier,
            album = album,
            suffix = suffix,
            suffixColor = suffixColor,
            favorite = favorite,
            showDivider = showDivider,
            onMore = onMore,
            navigationArrow = navigationArrow,
            footer = footer,
            isCurrentTrack = isCurrentTrack,
            showPressIndication = showPressIndication,
            fullWidthTrackPress = fullWidthTrackPress,
            menuAnchorState = menuAnchorState,
            trackRef = trackRef,
            copyNamesOnLongPress = copyNamesOnLongPress,
            hasAlternateVersion = hasAlternateVersion,
        )
        return
    }
    val alternateVersionAction = trackAlternateVersionAction(trackRef, title, hasAlternateVersion)
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val showAlbum = album != null && maxWidth >= 1000.dp
        Column {
            Box(Modifier.copyNameActions(
                trackName = title,
                artistName = artist,
                enabled = copyNamesOnLongPress,
            ).then(if (onClick != null) Modifier.clickable(
                interactionSource = null,
                indication = when {
                    fullWidthTrackPress -> trackRowPressIndication()
                    showPressIndication -> LocalIndication.current
                    else -> null
                },
                onClick = alternateVersionAction?.onRowClick ?: onClick,
            ) else Modifier)) {
                if (favorite) FavoriteTrackStar(
                    Modifier.align(Alignment.CenterStart).offset(x = (-18).dp),
                    tint = colors.secondaryContent,
                    luminosityBlend = false,
                )
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlayingImageView(imageUrl = image, isPlaying = false, trackRef = trackRef,
                        modifier = Modifier.size(songListCoverSize(true)).clip(LandscapeSongCoverShape))
                    Box(Modifier.weight(if (showAlbum) 1f else 1.25f)) {
                        MediaRowName(
                            text = title,
                            style = LocalTextStyle.current.copy(fontSize = 16.sp, fontWeight = FontWeight.Normal),
                            color = if (isCurrentTrack) colors.accent else colors.content,
                            copyAction = R.string.track_action_copy_name.takeIf { copyNamesOnLongPress },
                        )
                    }
                    Row(
                        Modifier.weight(if (showAlbum) 0.85f else 1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (alternateVersionAction != null) {
                            AlternateVersionBadge(onClick = alternateVersionAction.onBadgeClick)
                        }
                        MediaRowName(
                            text = artist,
                            style = LocalTextStyle.current.copy(fontSize = 16.sp),
                            color = colors.secondaryContent,
                            copyAction = R.string.artist_action_copy_name.takeIf { copyNamesOnLongPress },
                        )
                    }
                    if (showAlbum) Text(album.orEmpty(), Modifier.weight(1.35f),
                        fontSize = 16.sp, color = colors.secondaryContent,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(detail, Modifier.widthIn(min = 60.dp), textAlign = TextAlign.End,
                        fontSize = 16.sp, color = colors.secondaryContent, maxLines = 1, softWrap = false)
                    if (!suffix.isNullOrBlank()) Text(suffix, Modifier.widthIn(max = 100.dp),
                        fontSize = 16.sp, color = suffixColor ?: colors.secondaryContent,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (onMore != null) {
                        MediaRowMenuButton(Modifier.offset(x = (-8).dp), 18.dp, menuAnchorState, onMore)
                    } else if (navigationArrow) Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                        SfIcon("chevron.forward", null, size = 12.dp, tint = colors.separator)
                    }
                }
            }
            footer?.invoke()
            if (showDivider) HorizontalDivider(Modifier.padding(start = 60.dp), thickness = 0.5.dp, color = songListDividerColor())
        }
    }
}

@Composable
internal fun PortraitMediaRow(
    image: String,
    title: String,
    artist: String,
    detail: String,
    onClick: (() -> Unit)?,
    modifier: Modifier,
    album: String?,
    suffix: String?,
    suffixColor: Color?,
    favorite: Boolean,
    showDivider: Boolean,
    onMore: ((Rect) -> Unit)?,
    navigationArrow: Boolean,
    footer: (@Composable () -> Unit)?,
    isCurrentTrack: Boolean = false,
    showPressIndication: Boolean = false,
    fullWidthTrackPress: Boolean = false,
    menuAnchorState: GlasmioMenuAnchorState? = null,
    trackRef: TrackRef? = null,
    copyNamesOnLongPress: Boolean = false,
    hasAlternateVersion: Boolean = false,
) {
    val colors = LocalGlassColors.current
    val alternateVersionAction = trackAlternateVersionAction(trackRef, title, hasAlternateVersion)
    Box(modifier.fillMaxWidth()) {
        Column {
            Box(Modifier.copyNameActions(
                trackName = title,
                artistName = artist,
                enabled = copyNamesOnLongPress,
            ).then(if (onClick != null) Modifier.clickable(
                interactionSource = null,
                indication = when {
                    fullWidthTrackPress -> trackRowPressIndication()
                    showPressIndication -> LocalIndication.current
                    else -> null
                },
                onClick = alternateVersionAction?.onRowClick ?: onClick,
            ) else Modifier)) {
                if (favorite) FavoriteTrackStar(
                    Modifier.align(Alignment.CenterStart).offset(x = (-16).dp),
                    tint = colors.secondaryContent,
                    luminosityBlend = false,
                )
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlayingImageView(
                        imageUrl = image,
                        isPlaying = false,
                        trackRef = trackRef,
                        modifier = Modifier.size(songListCoverSize(false)).clip(PortraitSongCoverShape),
                    )
                    Column(Modifier.weight(1f)) {
                        MediaRowName(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal,
                            ),
                            color = if (isCurrentTrack) colors.accent else colors.content,
                            copyAction = R.string.track_action_copy_name.takeIf { copyNamesOnLongPress },
                        )
                        val subtitle = if (detail.isBlank() && suffix.isNullOrBlank()) {
                            artist
                        } else {
                            listOf(artist, detail, suffix.orEmpty()).filter { it.isNotBlank() }.joinToString(" · ")
                        }
                        if (subtitle.isNotBlank() || alternateVersionAction != null) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (alternateVersionAction != null) {
                                    AlternateVersionBadge(onClick = alternateVersionAction.onBadgeClick)
                                }
                                if (subtitle.isNotBlank()) MediaRowName(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, lineHeight = 17.sp),
                                    color = colors.secondaryContent,
                                    copyAction = R.string.artist_action_copy_name.takeIf { copyNamesOnLongPress },
                                    copyName = artist,
                                )
                            }
                        }
                    }
                    if (onMore != null) {
                        MediaRowMenuButton(Modifier.offset(x = 8.dp), 16.dp, menuAnchorState, onMore)
                    } else if (navigationArrow) Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                        SfIcon("chevron.forward", null, size = 12.dp, tint = colors.separator)
                    }
                }
            }
            if (showDivider) DetailTrackDivider()
        }
        footer?.let { progress ->
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth()) { progress() }
        }
    }
}

@Composable
internal fun MediaRowName(
    text: String,
    style: TextStyle,
    color: Color,
    @StringRes copyAction: Int?,
    copyName: String = text,
    maxLines: Int = 1,
    sharedMenuAnchor: GlasmioMenuAnchorState? = null,
) {
    if (copyAction == null || copyName.isBlank()) {
        Text(
            text = text,
            style = style,
            color = color,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
        )
        return
    }
    if (LocalCopyNamePopoverHost.current != null) {
        Text(
            text = text,
            modifier = Modifier.copyNameGesture(
                name = copyName,
                actionLabel = copyAction,
                accessibilityOnParent = true,
                sharedMenuAnchor = sharedMenuAnchor,
            ),
            style = style,
            color = color,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
        )
    } else {
        CopyNamePopover(
            name = copyName,
            actionLabel = copyAction,
            accessibilityOnParent = true,
            sharedMenuAnchor = sharedMenuAnchor,
        ) { gesture, _ ->
            Text(
                text = text,
                modifier = gesture,
                style = style,
                color = color,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MediaRowMenuButton(
    offset: Modifier,
    iconSize: Dp,
    menuAnchorState: GlasmioMenuAnchorState?,
    onMore: (Rect) -> Unit,
) {
    TrackMenuTrigger(menuAnchorState, onMore) { trigger, openMenu ->
        Box(
            Modifier.size(44.dp).then(offset)
                .clip(CircleShape)
                .then(trigger)
                .clickable(interactionSource = null, indication = null, onClick = openMenu),
            contentAlignment = Alignment.Center,
        ) {
            SfIcon("ellipsis", stringResource(R.string.more_actions_title), size = iconSize,
                tint = LocalGlassColors.current.secondaryContent)
        }
    }
}
