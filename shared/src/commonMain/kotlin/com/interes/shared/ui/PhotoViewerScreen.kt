package com.interes.shared.ui

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.interes.shared.model.Photo

@Composable
fun PhotoViewerScreen(
    photos: List<Photo>,
    pagerState: PagerState,
    modifier: Modifier = Modifier
) {
    PhotoViewerContent(
        photos = photos,
        pagerState = pagerState,
        modifier = modifier
    )
}