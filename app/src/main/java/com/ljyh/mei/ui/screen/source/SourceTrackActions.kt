package com.ljyh.mei.ui.screen.source

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ljyh.mei.R
import com.ljyh.mei.constants.MusicQuality
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.data.model.mediaId
import com.ljyh.mei.data.model.toMediaMetadata
import com.ljyh.mei.source.api.*
import com.ljyh.mei.ui.component.player.PlayerViewModel
import com.ljyh.mei.ui.component.playlist.TrackActionMenu
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.screen.social.NeteaseShareSheet
import io.github.neoruaa.glasmio.glass.GlasmioMenuAnchorState
import io.github.neoruaa.glasmio.glass.LocalBlurBackdrop
import io.github.neoruaa.glasmio.ui.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf

internal fun sourceContentSupported(ref: ContentRef, providers: List<MusicProvider>): Boolean {
    val provider = providers.firstOrNull { it.id == ref.providerId && it.status == SourceStatus.READY } ?: return false
    return when (ref) {
        is AlbumRef -> SourceCapability.ALBUM in provider.capabilities && provider is AlbumCapability
        is ArtistRef -> SourceCapability.ARTIST in provider.capabilities && provider is ArtistCapability
        is PlaylistRef -> SourceCapability.PLAYLIST in provider.capabilities && provider is PlaylistCapability
        is TrackRef -> SourceCapability.TRACK in provider.capabilities && provider is CatalogCapability
    }
}

@Composable
internal fun SourceTrackActionMessage(viewModel: SourceContentViewModel) {
    val message by viewModel.message.collectAsState()
    GlasmioAlertDialog(visible = message != null, onDismissRequest = viewModel::dismissMessage,
        title = "操作失败", message = message?.let(::sourceErrorMessage).orEmpty(),
        buttons = listOf(GlasmioAlertButtonSpec("好", onClick = viewModel::dismissMessage)))
}

/** Keep menu and sheet transitions out of the underlying browse page's composition. */
@Composable
internal fun SourceTrackActions(
    selection: MutableState<Pair<Track, Rect>?>,
    viewModel: SourceContentViewModel,
    menuAnchorState: GlasmioMenuAnchorState? = null,
    canDetachShareBackdrop: (() -> Boolean)? = null,
) {
    SourceTrackActions(
        selection = selection.value,
        onDismiss = { selection.value = null },
        viewModel = viewModel,
        menuAnchorState = menuAnchorState,
        canDetachShareBackdrop = canDetachShareBackdrop,
    )
}

@Composable
internal fun SourceTrackActions(
    selection: Pair<Track, Rect>?, onDismiss: () -> Unit, viewModel: SourceContentViewModel,
    playlist: SourcePlaylist? = null,
    menuAnchorState: GlasmioMenuAnchorState? = null,
    canDetachShareBackdrop: (() -> Boolean)? = null,
) {
    val context = LocalContext.current
    val playerViewModel: PlayerViewModel = hiltViewModel()
    var share by remember { mutableStateOf<MediaMetadata?>(null) }
    var playlistTrack by remember { mutableStateOf<TrackRef?>(null) }
    selection?.let { selected ->
        val metadata = selected.first.toMediaMetadata()
        key(selected.first.ref, selected.second) {
            SourceTrackActions(metadata = metadata, onDismiss = onDismiss, viewModel = viewModel,
                anchorBounds = selected.second, playerViewModel = playerViewModel, playlist = playlist,
                menuAnchorState = menuAnchorState,
                onAddToPlaylist = { playlistTrack = metadata.trackRef },
                onShare = { onDismiss(); share = metadata },
                onDownloadTrack = { quality -> playerViewModel.downloadSong(metadata, context, quality) })
        }
    }
    share?.let { metadata ->
        val presenter = LocalLibrarySharePresenter.current
        val backdrop = LocalBlurBackdrop.current
        val presenterBackdrop = if (presenter != null && presenter.backdrop === backdrop && canDetachShareBackdrop != null) {
            rememberLibraryShareBackdrop(backdrop) {
                presenter.isIdle() && canDetachShareBackdrop()
            }
        } else backdrop
        CompositionLocalProvider(LocalBlurBackdrop provides presenterBackdrop) {
            NeteaseShareSheet(metadata, onDismiss = { share = null })
        }
    }
    playlistTrack?.let { SourcePlaylistPickerSheet(it, onDismiss = { playlistTrack = null }) }
    SourceTrackActionMessage(viewModel)
}

/** Source capabilities supply actions to the original shared song menu. */
@Composable
internal fun SourceTrackActions(
    metadata: MediaMetadata,
    onDismiss: () -> Unit,
    viewModel: SourceContentViewModel,
    anchorBounds: Rect? = null,
    playerViewModel: PlayerViewModel,
    onAddToPlaylist: () -> Unit,
    onShare: (() -> Unit)? = null,
    onDownloadTrack: ((MusicQuality) -> Unit)? = null,
    playlist: SourcePlaylist? = null,
    menuAnchorState: GlasmioMenuAnchorState? = null,
    onNavigate: () -> Unit = {},
) {
    val nav = LocalNavController.current
    val providers by viewModel.providers.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val provider = providers.firstOrNull {
        it.id == metadata.trackRef.providerId && it.status == SourceStatus.READY
    }
    val editor = (provider as? PlaylistEditCapability)?.takeIf {
        SourceCapability.PLAYLIST_EDIT in provider.capabilities
    }
    val canAdd = !metadata.isLocal && !metadata.isPodcast &&
        PlaylistEditAction.ADD_TRACKS in editor?.playlistEditActions.orEmpty()
    val canRemove = playlist != null && playlist.ref.providerId == metadata.trackRef.providerId &&
        PlaylistEditAction.REMOVE_TRACKS in playlist.editableActions &&
        PlaylistEditAction.REMOVE_TRACKS in editor?.playlistEditActions.orEmpty()
    val likeProvider = provider?.takeIf {
        !metadata.isLocal && !metadata.isPodcast && SourceCapability.LIKE in it.capabilities &&
            it is LikeCapability && it.canReadLikes && it.canWriteLikes
    }
    val likeGeneration by playerViewModel.songLikeGeneration.collectAsState()
    val cachedLikes by playerViewModel.songLikes.collectAsState()
    val likeState by produceState<Pair<String?, Boolean>?>(null, metadata.trackRef, likeProvider, likeGeneration) {
        value = null
        if (likeProvider == null) return@produceState
        val revisions = (likeProvider as? AccountRevisionCapability)?.accountRevision ?: flowOf(null)
        revisions.collectLatest { revision ->
            value = null
            try {
                value = revision to viewModel.liked(metadata.trackRef, likeProvider, revision)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Keep the action disabled if the source cannot resolve the current state.
            }
        }
    }
    val favorite = likeProvider?.let { owner ->
        val state = likeState
        val liked = state?.second ?: (cachedLikes[metadata.mediaId] == true)
        GlasmioCascadingMenuItem(
            title = stringResource(if (liked) R.string.track_action_unfavorite else R.string.track_action_favorite),
            systemName = if (liked) "star.fill" else "star",
            enabled = !busy && state != null,
            onClick = {
                if (state != null) viewModel.setLiked(metadata.trackRef, !liked, owner, state.first)
            },
        )
    }
    fun openContent(ref: ContentRef) {
        navigateSourceContent(nav, ref)
        onNavigate()
    }
    val artistActions = metadata.artists.mapNotNull { artist ->
        val ref = artist.ref?.takeIf { sourceContentSupported(it, providers) } ?: return@mapNotNull null
        artist.name.takeIf(String::isNotBlank)?.let { name -> ref to (name to { openContent(ref) }) }
    }.distinctBy { it.first }.map { it.second }
    val album = metadata.album.ref?.takeIf {
        metadata.album.title.isNotBlank() && sourceContentSupported(it, providers)
    }
    key(metadata.trackRef, provider) {
        TrackActionMenu(
            targetTrack = metadata,
            anchorBounds = anchorBounds,
            menuAnchorState = menuAnchorState,
            onDismiss = onDismiss,
            onAddToPlaylist = onAddToPlaylist.takeIf { canAdd },
            onShare = onShare?.takeUnless { metadata.isLocal || metadata.isPodcast },
            onDownloadTrack = onDownloadTrack,
            isCreator = canRemove,
            deleteEnabled = !busy,
            onDelete = { playlist?.let { viewModel.removeTrack(it.ref, metadata.trackRef) } },
            favoriteAction = favorite,
            artistActions = artistActions,
            onOpenAlbum = album?.let { { openContent(it) } },
            playerViewModel = playerViewModel,
        )
    }
}
