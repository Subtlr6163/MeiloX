package com.ljyh.mei.source

import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.ljyh.mei.source.api.*
import com.ljyh.mei.source.core.BoundSourceFeatures
import com.ljyh.mei.source.core.ProviderRegistry
import com.ljyh.mei.source.core.SourceFeatureExecutor
import com.ljyh.mei.source.core.SourceRouter
import com.ljyh.mei.source.core.SourceSettings
import com.ljyh.mei.source.script.ScriptContentParser
import com.ljyh.mei.source.shared.SourceOnlineParser
import com.ljyh.mei.ui.component.item.TrackAlternateVersionPicker
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class TrackAlternateVersionTest {
    @Test fun scriptContentParserReadsHasAlternateVersionAndRejectsInvalidType() = runTest {
        val parser = ScriptContentParser("netease", 2, emptySet()) { it }
        val flagged = parser.track(json("""{"trackId":"42","title":"Song","artists":["Artist"],"hasAlternateVersion":true}"""))
        val plain = parser.track(json("""{"trackId":"43","title":"Song","artists":["Artist"]}"""))
        val explicitFalse = parser.track(json("""{"trackId":"44","title":"Song","artists":["Artist"],"hasAlternateVersion":false}"""))

        assertTrue(flagged.hasAlternateVersion)
        assertFalse(plain.hasAlternateVersion)
        assertFalse(explicitFalse.hasAlternateVersion)

        try {
            parser.track(json("""{"trackId":"45","title":"Song","artists":["Artist"],"hasAlternateVersion":"yes"}"""))
            fail("Expected invalid boolean to throw")
        } catch (error: SourceException) {
            assertEquals(SourceErrorCode.INVALID_RESPONSE, error.code)
        }
    }

    @Test fun onlineParserPreservesAlternateFlagForAccountAndWikiAndKeepsCloudPlayable() {
        val parser = SourceOnlineParser("netease", ProviderInstanceId("netease"))
        val recent = parser.song(json("""{"id":"42","name":"Song","artists":["Artist"],"album":"Album","durationMs":180000,"hasAlternateVersion":true}"""))
        assertTrue(recent.track.hasAlternateVersion)

        val wiki = parser.wiki(json("""{
            "memories":[],"tagGroups":[],"attributes":[],"associationGroups":[],"reviews":[],
            "similarSongs":[{"id":"43","title":"Similar","artist":"Artist","hasAlternateVersion":true}],
            "relatedPlaylists":[]
        }"""))
        assertTrue(wiki.similarSongs.single().track.hasAlternateVersion)

        val cloud = parser.cloud(json("""{"id":"42","name":"Uploaded","artist":"Artist","album":"Album","durationMs":180000,"fileSize":1024,"bitrate":320,"addTime":1700000000000,"hasAlternateVersion":true}"""))
        assertFalse(cloud.track.hasAlternateVersion)
    }

    @Test fun boundSourceFeaturesAlternateVersionValidatesResponses() = runTest {
        var response: JsonElement = JsonNull.INSTANCE
        var parsedOverride: Track? = null
        var invocations = 0
        val provider = FakeFeatureProvider(
            id = "netease",
            features = mapOf("playback" to setOf("alternateVersion")),
            onInvoke = { _, _, _ -> invocations++; response },
            onParseTrack = { value ->
                parsedOverride ?: Track(
                    TrackRef("netease", value.get("trackId").asString),
                    value.get("title").asString,
                    listOf(SourceArtist("Artist")),
                )
            },
        )
        val features = boundFeatures(provider)
        val origin = TrackRef("netease", "42")

        response = json("""{"trackId":"43","title":"Alternate"}""")
        val resolved = features.alternateVersion(origin)
        assertEquals(TrackRef("netease", "43"), resolved?.ref)
        assertEquals("Alternate", resolved?.title)
        assertEquals(1, invocations)

        response = JsonNull.INSTANCE
        assertNull(features.alternateVersion(origin))

        response = JsonParser.parseString("""["not-an-object"]""")
        assertInvalidResponse { features.alternateVersion(origin) }

        response = json("""{"trackId":"42","title":"Same"}""")
        assertInvalidResponse { features.alternateVersion(origin) }

        response = json("""{"trackId":"43","title":"Foreign"}""")
        parsedOverride = Track(TrackRef("other", "43"), "Foreign", listOf(SourceArtist("Artist")))
        assertInvalidResponse { features.alternateVersion(origin) }
        parsedOverride = null

        val unsupportedProvider = FakeFeatureProvider(id = "netease", features = emptyMap())
        assertNull(boundFeatures(unsupportedProvider).alternateVersion(origin))
    }

    @Test fun pickerRespectsLocalDownloadsFallbacksAndProviderLifecycle() {
        val netease = FakeFeatureProvider(
            id = "netease",
            features = mapOf("playback" to setOf("alternateVersion")),
        )
        val qq = FakePlayableProvider(id = "qq")
        val registry = ProviderRegistry(listOf(netease, qq))
        val origin = TrackRef("netease", "42")
        val picker = TrackAlternateVersionPicker(registry)

        // Default NetEase-only mode: shows badge and intercepts row clicks.
        assertTrue(picker.supports(origin))
        assertTrue(picker.shouldInterceptClick(origin))

        // Local download available: hides badge and never intercepts row clicks.
        picker.updateLocalRefs(setOf(origin))
        assertFalse(picker.supports(origin))
        assertFalse(picker.shouldInterceptClick(origin))
        picker.updateLocalRefs(emptySet())
        assertTrue(picker.shouldInterceptClick(origin))

        // Active cross-source fallback: keeps badge clickable, lets row click play via fallback.
        picker.updateSettings(SourceSettings(
            autoFallbackEnabled = true,
            fallbackOrder = listOf(qq.instanceId.value),
        ))
        assertTrue(picker.supports(origin))
        assertFalse(picker.shouldInterceptClick(origin))

        // Disabled fallback for this specific track restores row interception.
        picker.updateSettings(SourceSettings(
            autoFallbackEnabled = true,
            fallbackOrder = listOf(qq.instanceId.value),
            disabledTrackFallbacks = setOf(TrackRefCodec.encode(origin)),
        ))
        assertTrue(picker.shouldInterceptClick(origin))

        // Auto alternate versions in PlaybackResolver lets row click proceed without UI hijack.
        picker.updateSettings(SourceSettings(allowAlternateVersions = true))
        assertTrue(picker.supports(origin))
        assertFalse(picker.shouldInterceptClick(origin))

        // Unregistering the owning provider closes any open sheet request.
        picker.open(origin, "Song")
        assertNotNull(picker.request)
        registry.unregister(netease.instanceId)
        picker.refreshProviders()
        assertNull(picker.request)
        assertFalse(picker.supports(origin))
    }

    private fun boundFeatures(provider: MusicProvider): BoundSourceFeatures {
        val registry = ProviderRegistry(listOf(provider))
        val router = SourceRouter(registry)
        val executor = SourceFeatureExecutor(router)
        return BoundSourceFeatures(router, executor, provider, null, "test-alternate", SourceHostEffectHandler.Unsupported)
    }

    private suspend fun assertInvalidResponse(block: suspend () -> Unit) {
        try {
            block()
            fail("Expected INVALID_RESPONSE")
        } catch (error: SourceException) {
            assertEquals(SourceErrorCode.INVALID_RESPONSE, error.code)
        }
    }

    private fun json(raw: String): JsonObject = JsonParser.parseString(raw).asJsonObject

    private class FakeFeatureProvider(
        override val id: String,
        override val features: Map<String, Set<String>>,
        private val onInvoke: (String, String, JsonObject) -> JsonElement = { _, _, _ -> JsonNull.INSTANCE },
        private val onParseTrack: (JsonObject) -> Track = {
            Track(TrackRef(id, it.get("trackId").asString), it.get("title").asString, emptyList())
        },
    ) : MusicProvider, SourceFeatureCapability, SourceFeatureContentCapability {
        override val displayName: String = id
        override val capabilities: Set<SourceCapability> = setOf(SourceCapability.FEATURES)
        override suspend fun invokeFeature(feature: String, action: String, parameters: JsonObject): JsonElement =
            onInvoke(feature, action, parameters)
        override suspend fun featureArtwork(url: String): String = url
        override suspend fun parseFeatureTrack(value: JsonObject): Track = onParseTrack(value)
    }

    private class FakePlayableProvider(
        override val id: String,
    ) : MusicProvider, PlaybackCapability {
        override val displayName: String = id
        override val capabilities: Set<SourceCapability> = setOf(SourceCapability.PLAYBACK)
        override suspend fun resolve(ref: TrackRef, requestedQuality: String): AudioResource =
            AudioResource("https://example.com/audio.mp3", requestedQuality)
    }
}
