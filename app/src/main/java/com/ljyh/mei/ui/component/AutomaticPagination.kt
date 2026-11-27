package com.ljyh.mei.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

/** Requests the current page when its lazy list footer is composed. */
@Composable
internal fun AutomaticPagination(pageKey: Any?, onLoadMore: () -> Unit) {
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)
    LaunchedEffect(pageKey) { currentOnLoadMore() }
}
