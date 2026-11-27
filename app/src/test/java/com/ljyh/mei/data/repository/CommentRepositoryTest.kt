package com.ljyh.mei.data.repository

import androidx.paging.PagingSource.LoadParams
import androidx.paging.PagingSource.LoadResult
import com.ljyh.mei.data.model.api.CommentSortType
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.source.shared.OnlineComment
import com.ljyh.mei.source.shared.OnlineCommentPage
import com.ljyh.mei.source.shared.OnlineCommentUser
import com.ljyh.mei.ui.screen.comment.CommentComposerState
import com.ljyh.mei.ui.screen.comment.CommentLikeState
import com.ljyh.mei.ui.screen.comment.CommentPagingSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class CommentRepositoryTest {
    @Test
    fun composerAndAcceptedLikeStatesPreventInvalidActions() {
        assertFalse(CommentComposerState(draft = " \n ").canSend)
        assertFalse(CommentComposerState(draft = "Comment", isSending = true).canSend)
        assertTrue(CommentComposerState(draft = "Comment").canSend)
        assertEquals(CommentLikeState(true, 11), CommentLikeState(false, 10).toggled())
        assertEquals(CommentLikeState(false, 10), CommentLikeState(false, 10).toggled().toggled())
        assertEquals(CommentLikeState(false, 0), CommentLikeState(true, 0).toggled())
    }

    @Test
    fun commentPagingPreservesOrderAndAdvancesPastAnOverlappingPage() = runTest {
        var total = 0
        val source = CommentPagingSource(CommentSortType.HOT, loadPage = { pageNo, _, cursor ->
            when (pageNo) {
                1 -> {
                    assertEquals("", cursor)
                    Resource.Success(OnlineCommentPage(listOf(comment("a"), comment("a"), comment("b")), true, "normalHot#20", 5))
                }
                2 -> {
                    assertEquals("normalHot#20", cursor)
                    Resource.Success(OnlineCommentPage(listOf(comment("a"), comment("b")), true, "normalHot#78", 5))
                }
                else -> {
                    assertEquals("normalHot#78", cursor)
                    Resource.Success(OnlineCommentPage(listOf(comment("b"), comment("c"), comment("d")), false, null, 5))
                }
            }
        }, onTotalReceived = { total = it })

        val first = source.load(LoadParams.Refresh(null, 20, false)) as LoadResult.Page
        val second = source.load(LoadParams.Append(checkNotNull(first.nextKey), 20, false)) as LoadResult.Page
        val third = source.load(LoadParams.Append(checkNotNull(second.nextKey), 20, false)) as LoadResult.Page
        assertEquals(listOf("a", "b"), first.data.map(OnlineComment::commentId))
        assertTrue(second.data.isEmpty())
        assertEquals(listOf("c", "d"), third.data.map(OnlineComment::commentId))
        assertNull(third.nextKey)
        assertEquals(5, total)
    }

    @Test
    fun commentPagingStopsOnCursorCycleAndAllowsRetryAfterFailure() = runTest {
        var failAppend = true
        val source = CommentPagingSource(CommentSortType.RECOMMEND, loadPage = { pageNo, _, cursor ->
            when (pageNo) {
                1 -> Resource.Success(OnlineCommentPage(listOf(comment("a")), true, "1758493972274", 3))
                2 -> {
                    assertEquals("1758493972274", cursor)
                    if (failAppend) {
                        failAppend = false
                        Resource.Error("Network")
                    } else {
                        Resource.Success(OnlineCommentPage(listOf(comment("b")), true, "1724771422909", 3))
                    }
                }
                else -> {
                    assertEquals("1724771422909", cursor)
                    Resource.Success(OnlineCommentPage(listOf(comment("b")), true, "1758493972274", 3))
                }
            }
        })

        val first = source.load(LoadParams.Refresh(null, 20, false)) as LoadResult.Page
        val append = LoadParams.Append(checkNotNull(first.nextKey), 20, false)
        assertTrue(source.load(append) is LoadResult.Error)
        val second = source.load(append) as LoadResult.Page
        val third = source.load(LoadParams.Append(checkNotNull(second.nextKey), 20, false)) as LoadResult.Page
        assertEquals(listOf("b"), second.data.map(OnlineComment::commentId))
        assertTrue(third.data.isEmpty())
        assertNull(third.nextKey)
    }

    private fun comment(id: String) = OnlineComment(
        id, OnlineCommentUser("user", "Listener", null), "", 1L, "", false, 0, 0
    )
}
