package org.fossify.clock.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.duolingo.open.rtlviewpager.RtlViewPager

/**
 * Tabs are switched by tapping the bottom navigation only (elderly-friendly redesign,
 * docs/elderly-spec/design-principles.md) - accidental horizontal swipes are a common
 * mis-tap for this audience, so touch-driven paging is disabled here while
 * setCurrentItem() (used by the tab bar) keeps working.
 */
class NonSwipeableViewPager @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : RtlViewPager(context, attrs) {
    override fun onTouchEvent(event: MotionEvent) = false

    override fun onInterceptTouchEvent(event: MotionEvent) = false
}
