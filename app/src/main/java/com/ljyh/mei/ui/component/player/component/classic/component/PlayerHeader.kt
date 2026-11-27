package com.ljyh.mei.ui.component.player.component.classic.component

import com.ljyh.mei.ui.component.player.PlayerMetadataTrackSwipe
import com.ljyh.mei.data.model.supportsNeteaseSongActions

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import com.ljyh.mei.ui.component.player.component.rememberPlayerFavoriteMotion
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousRoundedRectangle
import com.ljyh.mei.R
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.ui.component.CopyNameAnchor
import com.ljyh.mei.ui.component.CopyNamePopover
import com.ljyh.mei.ui.component.copyNameActions
import com.ljyh.mei.ui.component.sheet.LocalPlayerSheet
import com.ljyh.mei.ui.component.player.LocalPlayerRoamingModeLabel
import com.ljyh.mei.ui.component.player.autoMixIncomingMetadataDominant
import com.ljyh.mei.ui.component.player.rememberPlayerSheetPresentation
import com.ljyh.mei.ui.component.player.component.applemusic.AppleMusicMarqueeText
import com.ljyh.mei.ui.component.player.component.applemusic.PlayerTitleWithMode
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.icons.SfSymbol

@Composable
fun PlayerHeader(
    modifier: Modifier,
    mediaMetadata: MediaMetadata,
    isLiked: Boolean,
    showLikeButton: Boolean = mediaMetadata.supportsNeteaseSongActions,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    onLikeClick: () -> Unit,
    iconColor: Color = Color.White,
    appleMusicStyle: Boolean = false,
    applyStatusBarPadding: Boolean = true,
    onTextPositioned: (LayoutCoordinates) -> Unit = {},
    onRowPositioned: (LayoutCoordinates) -> Unit = {},
    modeLabel: String? = LocalPlayerRoamingModeLabel.current,
) {
    var metadataWidth by remember { mutableIntStateOf(0) }
    val favoriteMotion = rememberPlayerFavoriteMotion(isLiked, mediaMetadata.id)
    val marqueeEnabled = rememberPlayerSheetPresentation(LocalPlayerSheet.current?.state).isExpanded
    val actionsEnabled = !autoMixIncomingMetadataDominant()
    val artistNames = mediaMetadata.artists.joinToString { it.name }
    val albumName = mediaMetadata.album.title
    val copyArtist = artistNames.isNotBlank()
    val titleAnchor = remember(appleMusicStyle) { CopyNameAnchor() }
    val subtitleAnchor = remember(appleMusicStyle) { CopyNameAnchor() }
    val subtitleCopyAction = if (copyArtist) R.string.artist_action_copy_name to artistNames
        else R.string.album_action_copy_name to albumName
    val metadataClick = if (appleMusicStyle) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            enabled = actionsEnabled,
            onClick = onClick,
        )
    } else Modifier.clickable(enabled = actionsEnabled, onClick = onClick)


    Row(
        modifier = modifier
            .then(if (applyStatusBarPadding) Modifier.statusBarsPadding() else Modifier)
            .fillMaxWidth()
            .copyNameActions(
                trackName = mediaMetadata.title,
                artistName = if (copyArtist) artistNames else null,
                albumName = if (copyArtist) null else albumName,
                enabled = actionsEnabled,
            )
            .then(metadataClick)
            .onGloballyPositioned(onRowPositioned),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧：标题和副标题
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(
                    end = (if (appleMusicStyle) 0.dp else 8.dp) +
                        (if (showLikeButton) 0.dp else 2.dp),
                )
                .onGloballyPositioned(onTextPositioned)
                .onSizeChanged { metadataWidth = it.width }
        ) {
            PlayerTitleWithMode(
                modeLabel, MaterialTheme.typography.titleMedium, Color.White.copy(alpha = 0.7f),
            ) { titleModifier ->
                CopyNamePopover(
                    name = mediaMetadata.title,
                    actionLabel = R.string.track_action_copy_name,
                    modifier = titleModifier,
                    enabled = actionsEnabled,
                    accessibilityOnParent = true,
                    textAnchor = titleAnchor,
                    observeVisibility = true,
                ) { copyGesture, popoverVisible ->
                    PlayerMetadataTrackSwipe(
                        modifier = copyGesture,
                        edgeInset = if (appleMusicStyle) 22.5.dp else 0.dp,
                        pageWidth = metadataWidth,
                    ) { track, allowMarquee ->
                        val visibleMetadata = track ?: mediaMetadata
                        val copyAnchor = titleAnchor.takeIf { allowMarquee }
                        val titleStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        if (appleMusicStyle) {
                            AppleMusicMarqueeText(
                                text = visibleMetadata.title,
                                style = titleStyle,
                                color = Color.White,
                                edgeWidth = 22.5.dp,
                                enabled = marqueeEnabled && allowMarquee && !popoverVisible,
                                copyAnchor = copyAnchor,
                            )
                        } else {
                            Text(
                                text = visibleMetadata.title,
                                modifier = copyAnchor?.modifier ?: Modifier,
                                style = titleStyle,
                                maxLines = 1,
                                color = Color.White,
                                overflow = TextOverflow.Ellipsis,
                                onTextLayout = { copyAnchor?.updateLayout(it) },
                            )
                        }
                    }
                }
            }

            if (!appleMusicStyle || artistNames.isNotEmpty() || albumName.isNotEmpty()) {
                CopyNamePopover(
                    name = subtitleCopyAction.second,
                    actionLabel = subtitleCopyAction.first,
                    enabled = actionsEnabled,
                    accessibilityOnParent = true,
                    textAnchor = subtitleAnchor,
                    observeVisibility = true,
                ) { copyGesture, popoverVisible ->
                    PlayerMetadataTrackSwipe(
                        modifier = copyGesture,
                        edgeInset = if (appleMusicStyle) 22.5.dp else 0.dp,
                        pageWidth = metadataWidth,
                    ) { track, allowMarquee ->
                        val visibleMetadata = track ?: mediaMetadata
                        val visibleArtists = visibleMetadata.artists.joinToString { it.name }
                        val visibleAlbum = visibleMetadata.album.title
                        val copyAnchor = subtitleAnchor.takeIf { allowMarquee }
                        val subTitleStyle = MaterialTheme.typography.titleMedium.copy(color = Color.White.copy(alpha = 0.7f))
                        if (appleMusicStyle) {
                            AppleMusicMarqueeText(
                                text = listOf(visibleArtists, visibleAlbum).filter { it.isNotEmpty() }.joinToString(" - "),
                                style = subTitleStyle,
                                color = subTitleStyle.color,
                                edgeWidth = 22.5.dp,
                                enabled = marqueeEnabled && allowMarquee && !popoverVisible,
                                copyAnchor = copyAnchor,
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                                    .basicMarquee(iterations = if (allowMarquee && !popoverVisible) Int.MAX_VALUE else 0)
                            ) {
                                if (visibleMetadata.artists.isNotEmpty()) {
                                    Text(
                                        text = visibleArtists,
                                        style = subTitleStyle,
                                        maxLines = 1,
                                        softWrap = false,
                                        onTextLayout = if (copyArtist) { layout -> copyAnchor?.updateLayout(layout) } else null,
                                        modifier = Modifier.clip(ContinuousRoundedRectangle(4.dp))
                                            .then(if (copyArtist) copyAnchor?.modifier ?: Modifier else Modifier),
                                    )
                                }
                                if (visibleMetadata.artists.isNotEmpty() && visibleAlbum.isNotEmpty()) {
                                    Text(
                                        text = " - ",
                                        style = subTitleStyle,
                                        modifier = Modifier.padding(horizontal = 2.dp)
                                    )
                                }
                                if (visibleAlbum.isNotEmpty()) {
                                    Text(
                                        text = visibleAlbum,
                                        style = subTitleStyle,
                                        maxLines = 1,
                                        softWrap = false,
                                        onTextLayout = if (copyArtist) null else { layout -> copyAnchor?.updateLayout(layout) },
                                        modifier = Modifier.clip(ContinuousRoundedRectangle(4.dp))
                                            .then(if (copyArtist) Modifier else copyAnchor?.modifier ?: Modifier),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(start = if (appleMusicStyle) 0.dp else 5.dp, end = 8.dp)
        ) {
            if (showLikeButton) {
                IconButton(
                    onClick = { favoriteMotion.click(isLiked, onLikeClick) },
                    enabled = actionsEnabled,
                    modifier = Modifier
                        .size(34.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .requiredSize(34.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        SfIcon(
                            modifier = Modifier.graphicsLayer {
                                scaleX = favoriteMotion.scale.value
                                scaleY = favoriteMotion.scale.value
                            },
                            symbol = if (isLiked) SfSymbol.StarFilled else SfSymbol.Star,
                            contentDescription = stringResource(R.string.app_tab_library_songs),
                            tint = iconColor.copy(alpha = 0.8f),
                            size = 22.dp,
                            weight = FontWeight.Bold
                        )
                    }
                }
            }
            // 右侧：更多按钮
            IconButton(
                onClick = onMoreClick,
                enabled = actionsEnabled,
                modifier = Modifier
                    .size(34.dp),
            ) {
                Box(
                    modifier = Modifier
                        .requiredSize(34.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    SfIcon(
                        symbol = SfSymbol.Ellipsis,
                        contentDescription = stringResource(R.string.more_actions_title),
                        tint = iconColor.copy(alpha = 0.8f),
                        size = 22.dp,
                    )
                }
            }
        }
    }
}
