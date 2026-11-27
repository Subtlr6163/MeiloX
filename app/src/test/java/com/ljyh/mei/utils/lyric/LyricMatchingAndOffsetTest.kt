package com.ljyh.mei.utils.lyric

import com.google.gson.Gson
import com.ljyh.mei.data.model.Lyric
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.data.model.qq.u.SearchResult
import com.ljyh.mei.data.model.qq.u.emptyData
import com.ljyh.mei.data.model.room.QQSong
import com.ljyh.mei.ui.model.LyricSource
import com.ljyh.mei.ui.model.LyricSourceData
import com.mocharealm.accompanist.lyrics.core.model.karaoke.KaraokeLine
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricMatchingAndOffsetTest {
    @Test
    fun neteaseYrcWithoutTranslationWinsOverMismatchedOrLowerPrioritySources() {
        val neteaseYrcOnly = Gson().fromJson(
            """{"code":200,"yrc":{"lyric":"[1000,800](1000,400,0)中文(1400,400,0)逐字"},"lrc":{"lyric":"[00:01.00]中文逐字"}}""",
            Lyric::class.java,
        )
        val qqQrc = LyricSourceData.QQMusic(
            emptyData.copy(lyric = "[1000,800]Other(1000,800)", qrcT = 1),
            isQRC = true,
        )

        val merged = mergeLyrics(listOf(LyricSourceData.NetEase(neteaseYrcOnly), qqQrc))
        assertEquals(LyricSource.NetEaseCloudMusic, merged.source)
        assertTrue(merged.isVerbatim)
        val line = merged.lyricLine.lines.single() as KaraokeLine.MainKaraokeLine
        assertEquals("中文逐字", line.syllables.joinToString("") { it.content })
    }

    @Test
    fun qqMatcherPrioritizesTitleWithArtistAndRejectsSameTitleDifferentArtist() {
        val metadata = MediaMetadata(
            id = 100L,
            title = "笼中爱 (Love in a Cage)",
            coverUrl = "",
            artists = listOf(MediaMetadata.Artist(1, "歌手A")),
            duration = 210_000L,
            album = MediaMetadata.Album(1, "专辑A"),
        )

        val keywords = QQLyricMatcher.searchKeywords(metadata)
        assertEquals("笼中爱 (Love in a Cage) 歌手A", keywords.first())
        assertEquals("笼中爱 歌手A", keywords[1])

        val wrongArtistSameTitle = qqItem(id = 1, title = "笼中爱", singer = "热门歌手B", interval = 210)
        val liveVersionSameArtist = qqItem(id = 2, title = "笼中爱 (Live)", singer = "歌手A", interval = 210)
        val rightArtistTranslatedSubtitle = qqItem(
            id = 3,
            title = "Love in a Cage",
            subtitle = "笼中爱",
            singer = "歌手A",
            album = "专辑A",
            interval = 211,
        )

        assertNull(QQLyricMatcher.selectBestMatch(metadata, listOf(wrongArtistSameTitle, liveVersionSameArtist)))
        val matched = QQLyricMatcher.selectBestMatch(
            metadata,
            listOf(wrongArtistSameTitle, liveVersionSameArtist, rightArtistTranslatedSubtitle),
        )
        assertNotNull(matched)
        assertEquals(3L, matched!!.id)

        assertFalse(
            QQLyricMatcher.isStoredSongCompatible(
                metadata,
                QQSong("100", "1", "笼中爱", "热门歌手B", "其他专辑", 210),
            ),
        )
        assertTrue(
            QQLyricMatcher.isStoredSongCompatible(
                metadata,
                QQSong("100", "3", "Love in a Cage", "歌手A", "专辑A", 211),
            ),
        )
    }

    @Test
    fun headerOffsetTagShiftsLrcYrcQrcAndTranslationsConsistently() {
        val lrc = LRCParser.parse(
            "[offset:500]\n[00:02.00]主歌第一句",
            "[offset:0]\n[00:01.50]First verse",
        )
        val lrcLine = lrc.lines.single() as SyncedLine
        assertEquals(1_500, lrcLine.start)
        assertEquals("First verse", lrcLine.translation)

        val qrc = QRCParser.parse(
            "[offset:-300]\n[1000,600]唱(1000,300)词(1300,300)",
            "[offset:0]\n[00:01.30]Translated",
        )
        val qrcLine = qrc.lines.single() as KaraokeLine.MainKaraokeLine
        assertEquals(1_300, qrcLine.start)
        assertEquals(1_900, qrcLine.end)
        assertEquals("Translated", qrcLine.translation)

        val yrc = YRCParser.parse(
            "[offset:200]\n[1000,500](1000,500,0)字",
            "[00:00.80]Word",
        )
        val yrcLine = yrc.lines.single() as KaraokeLine.MainKaraokeLine
        assertEquals(800, yrcLine.start)
        assertEquals(1_300, yrcLine.end)
        assertEquals("Word", yrcLine.translation)
    }

    private fun qqItem(
        id: Long,
        title: String,
        singer: String,
        interval: Long,
        subtitle: String = "",
        album: String = "",
    ): SearchResult.Request.Data.Body.ItemSong = Gson().fromJson(
        """{
            "id": $id,
            "name": ${Gson().toJson(title)},
            "title": ${Gson().toJson(title)},
            "subtitle": ${Gson().toJson(subtitle)},
            "interval": $interval,
            "singer": [{"id": 1, "mid": "s1", "name": ${Gson().toJson(singer)}, "pmid": "", "title": ${Gson().toJson(singer)}, "type": 0, "uin": 0}],
            "album": {"id": 1, "mid": "a1", "name": ${Gson().toJson(album)}, "pmid": "", "subtitle": "", "time_public": "", "title": ${Gson().toJson(album)}}
        }""",
        SearchResult.Request.Data.Body.ItemSong::class.java,
    )
}
