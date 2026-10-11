package ir.farimod.app.hooks

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Port of `app/hooks/useAutoScroll.ts` (`UseAutoScroll`).
 *
 * Every [intervalMs] the pager moves to `(lastAutoPage + 1) % totalPages`.
 * Like RN, the page counter is kept in a ref that is only advanced by the timer (manual swipes do not change it)
 * and the effect only restarts when [totalPages] / [intervalMs] change.
 *
 * @param setPage RN `pagerRef.current?.setPage(next)`
 * @param setCurrentPage RN `setCurrentPage(next)` (dot indicator state)
 */
@Composable
fun useAutoScroll(
    setPage: suspend (Int) -> Unit,
    totalPages: Int,
    intervalMs: Long,
    setCurrentPage: (Int) -> Unit
) {
    val currentPageRef = remember { IntArray(1) }
    val setPageState by rememberUpdatedState(setPage)
    val setCurrentPageState by rememberUpdatedState(setCurrentPage)

    LaunchedEffect(totalPages, intervalMs) {
        if (totalPages <= 1) return@LaunchedEffect

        while (true) {
            delay(intervalMs)
            val next = (currentPageRef[0] + 1) % totalPages
            launch { setPageState(next) }
            currentPageRef[0] = next
            // setState only for dot-indicator rendering — NOT for interval logic
            setCurrentPageState(next)
        }
    }
}

/** Same as above for a Compose [PagerState] (RN `PagerView` ref). */
@Composable
fun useAutoScroll(
    pagerState: PagerState,
    totalPages: Int,
    intervalMs: Long,
    setCurrentPage: (Int) -> Unit
) {
    useAutoScroll(
        setPage = { page -> pagerState.animateScrollToPage(page) },
        totalPages = totalPages,
        intervalMs = intervalMs,
        setCurrentPage = setCurrentPage
    )
}
