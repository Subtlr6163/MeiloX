package com.ljyh.mei.data.model.weapi

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.ljyh.mei.source.api.ProviderIds
import com.ljyh.mei.source.api.ProviderInstanceId
import com.ljyh.mei.source.shared.SourceOnlineParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CommentModelTest {
    @Test
    fun allowsNullIdentityIconUrlFromNeteaseComments() {
        val avatarDetail = Gson().fromJson(
            """{"identityIconUrl":null,"identityLevel":0,"userType":0}""",
            AvatarDetail::class.java,
        )

        assertNull(avatarDetail.identityIconUrl)
        avatarDetail.hashCode()
    }

    @Test
    fun keepsVoiceMetadataOnOtherwiseEmptyComments() {
        val parser = SourceOnlineParser(ProviderIds.NETEASE, ProviderInstanceId("fixture:netease"))
        val body = JsonParser.parseString("""
            {"commentId":"42","user":{"userId":"2","nickname":"Test"},"liked":false}
        """).asJsonObject
        assertNull(parser.comment(body).voiceDurationMs)
        body.add("voiceInfo", JsonParser.parseString("""{"durationMs":5200}"""))
        assertEquals(5200L, parser.comment(body).voiceDurationMs)
        body.remove("voiceInfo")
        body.addProperty("voiceNosKey", "fixture/audio")
        assertEquals(0L, parser.comment(body).voiceDurationMs)
    }
}
