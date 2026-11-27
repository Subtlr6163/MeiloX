package com.ljyh.mei.ui.component.item

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousRoundedRectangle
import com.ljyh.mei.R
import com.ljyh.mei.di.AppDatabase
import com.ljyh.mei.source.api.PlaybackCapability
import com.ljyh.mei.source.api.ProviderInstanceId
import com.ljyh.mei.source.api.SourceCapability
import com.ljyh.mei.source.api.SourceFeatureCapability
import com.ljyh.mei.source.api.SourceHostEffectHandler
import com.ljyh.mei.source.api.SourceStatus
import com.ljyh.mei.source.api.Track
import com.ljyh.mei.source.api.TrackRef
import com.ljyh.mei.source.api.TrackRefCodec
import com.ljyh.mei.source.core.BoundSourceFeatures
import com.ljyh.mei.source.core.ProviderRegistry
import com.ljyh.mei.source.core.SourceFeatureClient
import com.ljyh.mei.source.core.SourcePreferences
import com.ljyh.mei.source.core.SourceSettings
import com.ljyh.mei.ui.component.playlist.PlayingImageView
import com.ljyh.mei.ui.component.sheet.MeiloXBottomSheet
import com.ljyh.mei.ui.component.sheet.rememberMeiloXSheetDismissal
import com.ljyh.mei.utils.smallImage
import io.github.neoruaa.glasmio.core.LocalGlassColors
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.icons.SfSymbol
import io.github.neoruaa.glasmio.ui.GlasmioGroupedList
import io.github.neoruaa.glasmio.ui.GlasmioListRow
import io.github.neoruaa.glasmio.ui.GlasmioSheetToolbarButton
import io.github.neoruaa.glasmio.ui.GlasmioSheetTopToolbar
import io.github.neoruaa.glasmio.ui.LocalGlasmioSheetContentPadding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class TrackAlternateVersionRequest(
    val ref: TrackRef,
    val title: String,
    val onSelectTrack: ((Track) -> Unit)? = null,
)

@Immutable
internal data class TrackAlternateVersionAction(
    val onRowClick: (() -> Unit)?,
    val onBadgeClick: (() -> Unit)?,
)

@Stable
internal class TrackAlternateVersionPicker internal constructor(
    private val registry: ProviderRegistry,
    initialSettings: SourceSettings = SourceSettings(),
    initialLocalRefs: Set<TrackRef> = emptySet(),
) {
    var request by mutableStateOf<TrackAlternateVersionRequest?>(null)
        private set

    private var supportedProviders by mutableStateOf(computeSupportedProviders(registry))
    private var settings by mutableStateOf(initialSettings)
    private var localRefs by mutableStateOf(initialLocalRefs)
    private var playableFallbackIds by mutableStateOf(computePlayableFallbackIds(registry, initialSettings))

    internal fun refreshProviders() {
        val nextProviders = computeSupportedProviders(registry)
        if (nextProviders != supportedProviders) {
            supportedProviders = nextProviders
            request?.let { current ->
                if (current.ref.providerId !in nextProviders) request = null
            }
        }
        val nextFallbacks = computePlayableFallbackIds(registry, settings)
        if (nextFallbacks != playableFallbackIds) {
            playableFallbackIds = nextFallbacks
        }
    }

    internal fun updateSettings(next: SourceSettings) {
        if (next != settings) {
            settings = next
            playableFallbackIds = computePlayableFallbackIds(registry, next)
        }
    }

    internal fun updateLocalRefs(next: Set<TrackRef>) {
        if (next != localRefs) {
            localRefs = next
        }
    }

    fun supports(ref: TrackRef): Boolean =
        ref.providerId in supportedProviders && ref !in localRefs

    fun shouldInterceptClick(ref: TrackRef): Boolean {
        if (!supports(ref)) return false
        val encoded = TrackRefCodec.encode(ref)
        if (encoded in settings.disabledTrackFallbacks) return true
        if (settings.allowAlternateVersions) return false
        if (settings.autoFallbackEnabled && playableFallbackIds.any { it != ref.providerId }) return false
        return true
    }

    fun open(ref: TrackRef, title: String, onSelectTrack: ((Track) -> Unit)? = null) {
        request = TrackAlternateVersionRequest(ref, title, onSelectTrack)
    }

    fun close(origin: TrackAlternateVersionRequest) {
        if (request === origin) request = null
    }

    private companion object {
        fun computeSupportedProviders(registry: ProviderRegistry): Set<String> =
            registry.providers.value.filter {
                it.status == SourceStatus.READY &&
                    "alternateVersion" in (it as? SourceFeatureCapability)?.features?.get("playback").orEmpty()
            }.mapTo(mutableSetOf()) { it.id }

        fun computePlayableFallbackIds(registry: ProviderRegistry, settings: SourceSettings): Set<String> {
            if (!settings.autoFallbackEnabled) return emptySet()
            return settings.fallbackOrder.mapNotNullTo(mutableSetOf()) { id ->
                registry.instance(ProviderInstanceId(id))?.takeIf {
                    it.status == SourceStatus.READY &&
                        it is PlaybackCapability &&
                        SourceCapability.PLAYBACK in it.capabilities
                }?.id
            }
        }
    }
}

internal val LocalTrackAlternateVersionPicker = compositionLocalOf<TrackAlternateVersionPicker?> { null }

@Composable
internal fun rememberTrackAlternateVersionPicker(
    registry: ProviderRegistry,
    preferences: SourcePreferences,
    database: AppDatabase,
): TrackAlternateVersionPicker {
    val picker = remember(registry) {
        TrackAlternateVersionPicker(registry, initialSettings = preferences.latest ?: SourceSettings())
    }
    LaunchedEffect(picker, registry) {
        registry.revision.collect { picker.refreshProviders() }
    }
    LaunchedEffect(picker, preferences) {
        preferences.settings.collect { picker.updateSettings(it) }
    }
    LaunchedEffect(picker, database) {
        database.songDao().getLocalSongs()
            .map { songs ->
                songs.mapTo(mutableSetOf()) { song ->
                    TrackRef(song.providerId, song.providerTrackId.ifEmpty { song.id })
                }
            }
            .distinctUntilChanged()
            .collect { picker.updateLocalRefs(it) }
    }
    return picker
}

@Composable
internal fun trackAlternateVersionAction(
    ref: TrackRef?,
    title: String,
    hasAlternateVersion: Boolean,
    onSelectTrack: ((Track) -> Unit)? = null,
): TrackAlternateVersionAction? {
    if (!hasAlternateVersion || ref == null) return null
    val picker = LocalTrackAlternateVersionPicker.current ?: return null
    if (!picker.supports(ref)) return null
    val open = remember(picker, ref, title, onSelectTrack) {
        { picker.open(ref, title, onSelectTrack) }
    }
    val intercept = picker.shouldInterceptClick(ref)
    return remember(open, intercept) {
        if (intercept) TrackAlternateVersionAction(onRowClick = open, onBadgeClick = null)
        else TrackAlternateVersionAction(onRowClick = null, onBadgeClick = open)
    }
}

@Composable
internal fun AlternateVersionBadge(
    modifier: Modifier = Modifier,
    color: Color = LocalGlassColors.current.secondaryContent,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(4.dp)
    Text(
        text = stringResource(R.string.track_alternate_version_badge),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .border(Dp.Hairline, color, shape)
            .padding(horizontal = 4.dp),
    )
}

@Composable
internal fun AlternateVersionSubtitle(
    showBadge: Boolean,
    color: Color = LocalGlassColors.current.secondaryContent,
    onBadgeClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    if (!showBadge) {
        content()
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        AlternateVersionBadge(color = color, onClick = onBadgeClick)
        content()
    }
}

@Composable
internal fun TrackAlternateVersionSheet(
    picker: TrackAlternateVersionPicker,
    client: SourceFeatureClient,
    currentRoute: () -> String?,
    onPlay: (Track) -> Unit,
) {
    val origin = picker.request ?: return
    val dismissal = rememberMeiloXSheetDismissal(
        onDismiss = { picker.close(origin) },
        skipPartiallyExpanded = true,
    )
    val scope = rememberCoroutineScope()
    var lookup by remember(origin) { mutableStateOf<Result<Pair<BoundSourceFeatures, Track?>>?>(null) }
    var retry by remember(origin) { mutableIntStateOf(0) }
    var submitting by remember(origin) { mutableStateOf(false) }
    LaunchedEffect(origin) {
        val openedRoute = currentRoute()
        snapshotFlow { currentRoute() }.first { it != openedRoute }
        picker.close(origin)
    }
    LaunchedEffect(origin, retry) {
        var bound: BoundSourceFeatures? = null
        try {
            val features = client.bind(origin.ref.providerId, "track-alternate-version", SourceHostEffectHandler.Unsupported)
            bound = features
            lookup = Result.success(features to features.alternateVersion(origin.ref))
            features.awaitInvalidation()
        } catch (cancelled: CancellationException) {
            if (currentCoroutineContext().isActive) picker.close(origin)
            throw cancelled
        } catch (error: Exception) {
            lookup = Result.failure(error)
            try {
                bound?.awaitInvalidation()
            } catch (cancelled: CancellationException) {
                if (currentCoroutineContext().isActive) picker.close(origin)
                throw cancelled
            }
        }
    }
    MeiloXBottomSheet(
        onDismissRequest = dismissal::onDismissed,
        sheetState = dismissal.state,
        title = origin.title,
        fitContent = true,
        topBar = {
            GlasmioSheetTopToolbar(title = origin.title, actions = {
                GlasmioSheetToolbarButton(onClick = { dismissal.requestDismiss() }) {
                    SfIcon(SfSymbol.Close, stringResource(R.string.cancel))
                }
            })
        },
    ) {
        val padding = LocalGlasmioSheetContentPadding.current
        Column(Modifier.fillMaxWidth().padding(
            start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 16.dp,
        )) {
            val result = lookup
            val match = result?.getOrNull()
            val track = match?.second
            if (result == null) {
                Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            } else GlasmioGroupedList {
                when {
                    result.isFailure -> GlasmioListRow(
                        title = stringResource(R.string.load_failed),
                        trailing = { Text(stringResource(R.string.retry)) },
                        onClick = {
                            lookup = null
                            retry++
                        },
                    )
                    track == null -> GlasmioListRow(title = stringResource(R.string.track_alternate_version_empty))
                    else -> {
                        val artist = track.artists.joinToString(" / ") { it.name }
                        GroupedSongRow(
                            title = track.title,
                            artist = artist,
                            subtitle = artist.takeIf(String::isNotBlank),
                            leading = {
                                PlayingImageView(
                                    imageUrl = track.artworkUrl.smallImage(),
                                    isPlaying = false,
                                    trackRef = track.ref,
                                    modifier = Modifier.size(44.dp).clip(ContinuousRoundedRectangle(10.dp)),
                                )
                            },
                            trailing = { SfIcon("play.fill", stringResource(R.string.player_play), size = 18.dp) },
                            showTopSeparator = false,
                            onClick = {
                                if (submitting || dismissal.isDismissing) return@GroupedSongRow
                                submitting = true
                                scope.launch {
                                    try {
                                        match.first.ensureCurrent()
                                        if (!dismissal.isDismissing && picker.request === origin) {
                                            (origin.onSelectTrack ?: onPlay)(track)
                                            dismissal.requestDismiss()
                                        }
                                    } catch (cancelled: CancellationException) {
                                        if (currentCoroutineContext().isActive) picker.close(origin)
                                        throw cancelled
                                    } finally {
                                        submitting = false
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
