package com.buildwclaude.alarm.ui.edit

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/**
 * A vertical scroll wheel. The item nearest the centre is "selected"; scrolling snaps to
 * whole items and reports the centred index via [onCentered]. Used for hours and minutes so
 * the picker literally scrolls, repainting the sky live as it moves.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    count: Int,
    initialIndex: Int,
    label: (Int) -> String,
    onCentered: (Int) -> Unit,
    color: Color,
    modifier: Modifier = Modifier,
    rowHeight: Dp = 56.dp,
    visibleRows: Int = 3,
) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex.coerceIn(0, (count - 1).coerceAtLeast(0)))
    val fling = rememberSnapFlingBehavior(lazyListState = state)

    val centeredIndex by remember {
        derivedStateOf {
            val li = state.layoutInfo
            if (li.visibleItemsInfo.isEmpty()) {
                initialIndex
            } else {
                val viewportCenter = (li.viewportStartOffset + li.viewportEndOffset) / 2f
                li.visibleItemsInfo.minByOrNull { abs((it.offset + it.size / 2f) - viewportCenter) }?.index
                    ?: initialIndex
            }
        }
    }

    LaunchedEffect(centeredIndex) { onCentered(centeredIndex.coerceIn(0, count - 1)) }

    Box(modifier.height(rowHeight * visibleRows), contentAlignment = Alignment.Center) {
        LazyColumn(
            state = state,
            flingBehavior = fling,
            contentPadding = PaddingValues(vertical = rowHeight * ((visibleRows - 1) / 2)),
        ) {
            items(count) { i ->
                val selected = i == centeredIndex
                val distance = abs(i - centeredIndex)
                Box(
                    Modifier.height(rowHeight).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label(i),
                        color = color.copy(alpha = if (selected) 1f else (0.4f / (distance)).coerceIn(0.18f, 0.5f)),
                        textAlign = TextAlign.Center,
                        style = LocalTextStyle.current.copy(
                            fontSize = if (selected) 44.sp else 30.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        ),
                    )
                }
            }
        }
    }
}
