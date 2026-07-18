package org.fossify.clock.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Icon
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.WindowManager
import androidx.core.graphics.drawable.toDrawable
import me.grantland.widget.AutofitHelper
import org.fossify.clock.BuildConfig
import org.fossify.clock.R
import org.fossify.clock.adapters.ViewPagerAdapter
import org.fossify.clock.databinding.ActivityMainBinding
import org.fossify.clock.extensions.alarmController
import org.fossify.clock.extensions.config
import org.fossify.clock.extensions.getEnabledAlarms
import org.fossify.clock.extensions.handleFullScreenNotificationsPermission
import org.fossify.clock.extensions.updateWidgets
import org.fossify.clock.helpers.OPEN_TAB
import org.fossify.clock.helpers.STOPWATCH_SHORTCUT_ID
import org.fossify.clock.helpers.STOPWATCH_TOGGLE_ACTION
import org.fossify.clock.helpers.TABS_COUNT
import org.fossify.clock.helpers.TAB_ALARM
import org.fossify.clock.helpers.TAB_ALARM_INDEX
import org.fossify.clock.helpers.TAB_CLOCK
import org.fossify.clock.helpers.TAB_CLOCK_INDEX
import org.fossify.clock.helpers.TAB_STOPWATCH
import org.fossify.clock.helpers.TAB_STOPWATCH_INDEX
import org.fossify.clock.helpers.TAB_TIMER
import org.fossify.clock.helpers.TAB_TIMER_INDEX
import org.fossify.clock.helpers.TOGGLE_STOPWATCH
import org.fossify.commons.databinding.BottomTablayoutItemBinding
import org.fossify.commons.extensions.appLaunched
import org.fossify.commons.extensions.applyColorFilter
import org.fossify.commons.extensions.convertToBitmap
import org.fossify.commons.extensions.getBottomNavigationBackgroundColor
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.onPageChangeListener
import org.fossify.commons.extensions.onTabSelectionChanged
import org.fossify.commons.extensions.shortcutManager
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.updateBottomTabItemColors
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.ensureBackgroundThread
import java.time.temporal.WeekFields
import java.util.Locale

class MainActivity : SimpleActivity() {
    companion object {
        private val TAB_LABELS = arrayOf(
            R.string.clock,
            org.fossify.commons.R.string.alarm,
            R.string.stopwatch,
            R.string.timer
        )
    }

    private var storedTextColor = 0
    private var storedBackgroundColor = 0
    private var storedPrimaryColor = 0
    private val binding: ActivityMainBinding by viewBinding(ActivityMainBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        appLaunched(BuildConfig.APPLICATION_ID)

        setupEdgeToEdge(padBottomImeAndSystem = listOf(binding.mainTabsHolder))

        storeStateVariables()
        initFragments()
        setupTabs()
        updateWidgets()
        migrateFirstDayOfWeek()
        ensureBackgroundThread {
            alarmController.rescheduleEnabledAlarms()
        }

        getEnabledAlarms { enabledAlarms ->
            if (!enabledAlarms.isNullOrEmpty()) {
                handleFullScreenNotificationsPermission {
                    if (!it) {
                        toast(org.fossify.commons.R.string.notifications_disabled)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.mainAppbar, topBarColor = getProperBackgroundColor())
        // Header always reads "Clock" regardless of the active tab - not per-tab.
        binding.mainToolbar.title = getString(R.string.clock)
        val configTextColor = getProperTextColor()
        if (storedTextColor != configTextColor) {
            getInactiveTabIndexes(binding.viewPager.currentItem).forEach {
                binding.mainTabsHolder.getTabAt(it)?.icon?.applyColorFilter(configTextColor)
            }
        }

        val configBackgroundColor = getProperBackgroundColor()
        if (storedBackgroundColor != configBackgroundColor) {
            binding.mainTabsHolder.background = configBackgroundColor.toDrawable()
        }

        val configPrimaryColor = getProperPrimaryColor()
        if (storedPrimaryColor != configPrimaryColor) {
            binding.mainTabsHolder.setSelectedTabIndicatorColor(getProperPrimaryColor())
            binding.mainTabsHolder.getTabAt(binding.viewPager.currentItem)?.icon
                ?.applyColorFilter(getProperPrimaryColor())
        }

        if (config.preventPhoneFromSleeping) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        setupTabColors()
        checkShortcuts()
    }

    @SuppressLint("NewApi")
    private fun checkShortcuts() {
        val appIconColor = config.appIconColor
        if (config.lastHandledShortcutColor != appIconColor) {
            val stopWatchShortcutInfo = getLaunchStopwatchShortcut(appIconColor)

            try {
                shortcutManager.dynamicShortcuts = listOf(stopWatchShortcutInfo)
                config.lastHandledShortcutColor = appIconColor
            } catch (ignored: Exception) {
            }
        }
    }

    @SuppressLint("NewApi")
    private fun getLaunchStopwatchShortcut(appIconColor: Int): ShortcutInfo {
        val newEvent = getString(R.string.start_stopwatch)
        val drawable = resources.getDrawable(R.drawable.shortcut_stopwatch)
        (drawable as LayerDrawable)
            .findDrawableByLayerId(R.id.shortcut_stopwatch_background)
            .applyColorFilter(appIconColor)
        val bmp = drawable.convertToBitmap()

        val intent = Intent(this, SplashActivity::class.java).apply {
            putExtra(OPEN_TAB, TAB_STOPWATCH)
            putExtra(TOGGLE_STOPWATCH, true)
            action = STOPWATCH_TOGGLE_ACTION
        }

        return ShortcutInfo.Builder(this, STOPWATCH_SHORTCUT_ID)
            .setShortLabel(newEvent)
            .setLongLabel(newEvent)
            .setIcon(Icon.createWithBitmap(bmp))
            .setIntent(intent)
            .build()
    }

    override fun onPause() {
        super.onPause()
        storeStateVariables()
        if (config.preventPhoneFromSleeping) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        config.lastUsedViewPagerPage = binding.viewPager.currentItem
    }

    override fun onNewIntent(intent: Intent) {
        if (intent.extras?.containsKey(OPEN_TAB) == true) {
            val tabToOpen = intent.getIntExtra(OPEN_TAB, TAB_CLOCK)
            binding.viewPager.setCurrentItem(getTabIndex(tabToOpen), false)
            if (tabToOpen == TAB_STOPWATCH) {
                if (intent.getBooleanExtra(TOGGLE_STOPWATCH, false)) {
                    (binding.viewPager.adapter as ViewPagerAdapter).startStopWatch()
                }
            }
        }
        super.onNewIntent(intent)
    }

    private fun storeStateVariables() {
        storedTextColor = getProperTextColor()
        storedBackgroundColor = getProperBackgroundColor()
        storedPrimaryColor = getProperPrimaryColor()
    }

    fun updateClockTabAlarm() {
        getViewPagerAdapter()?.updateClockTabAlarm()
    }

    private fun getViewPagerAdapter() = binding.viewPager.adapter as? ViewPagerAdapter

    private fun initFragments() {
        val viewPagerAdapter = ViewPagerAdapter(supportFragmentManager)
        binding.viewPager.adapter = viewPagerAdapter
        binding.viewPager.onPageChangeListener {
            binding.mainTabsHolder.getTabAt(it)?.select()
        }

        val tabToOpen = intent.getIntExtra(OPEN_TAB, config.defaultTab)
        intent.removeExtra(OPEN_TAB)

        if (tabToOpen == TAB_STOPWATCH) {
            config.toggleStopwatch = intent.getBooleanExtra(TOGGLE_STOPWATCH, false)
        }

        binding.viewPager.offscreenPageLimit = TABS_COUNT - 1
        binding.viewPager.currentItem = getTabIndex(tabToOpen)
    }

    private fun setupTabs() {
        binding.mainTabsHolder.removeAllTabs()
        val tabDrawables = arrayOf(
            R.drawable.ic_clock_vector,
            R.drawable.ic_alarm_vector,
            R.drawable.ic_stopwatch_vector,
            R.drawable.ic_hourglass_vector
        )
        tabDrawables.forEachIndexed { i, drawableId ->
            binding.mainTabsHolder.newTab()
                .setCustomView(org.fossify.commons.R.layout.bottom_tablayout_item)
                .apply tab@{
                    customView?.let { BottomTablayoutItemBinding.bind(it) }?.apply {
                        tabItemIcon.setImageDrawable(getDrawable(drawableId))
                        tabItemLabel.setText(TAB_LABELS[i])
                        AutofitHelper.create(tabItemLabel)
                        binding.mainTabsHolder.addTab(this@tab)
                    }
                }
        }

        binding.mainTabsHolder.onTabSelectionChanged(
            tabUnselectedAction = {
                updateBottomTabItemColors(
                    view = it.customView,
                    isActive = false,
                    drawableId = getDeselectedTabDrawableIds()[it.position]
                )
                applyTabColorOverride(it.customView, isActive = false)
            },
            tabSelectedAction = {
                binding.viewPager.currentItem = it.position
                updateBottomTabItemColors(
                    view = it.customView,
                    isActive = true,
                    drawableId = getDeselectedTabDrawableIds()[it.position]
                )
                applyTabColorOverride(it.customView, isActive = true)
            }
        )
    }

    // Bottom nav: both states use the same outline icon - only the tint differs. Unselected
    // tabs are purple (matching the app's outlined-purple "at rest" language elsewhere); the
    // selected tab is black, so it's unmistakable which tab is active without switching to a
    // filled icon shape.
    private fun applyTabColorOverride(view: android.view.View?, isActive: Boolean) {
        val binding = view?.let { BottomTablayoutItemBinding.bind(it) } ?: return
        val color = if (isActive) android.graphics.Color.BLACK else getProperPrimaryColor()
        binding.tabItemIcon.applyColorFilter(color)
        binding.tabItemLabel.setTextColor(color)
    }

    private fun setupTabColors() {
        val activeView = binding.mainTabsHolder.getTabAt(binding.viewPager.currentItem)?.customView
        updateBottomTabItemColors(
            view = activeView,
            isActive = true,
            drawableId = getDeselectedTabDrawableIds()[binding.viewPager.currentItem]
        )
        applyTabColorOverride(activeView, isActive = true)

        getInactiveTabIndexes(binding.viewPager.currentItem).forEach { index ->
            val inactiveView = binding.mainTabsHolder.getTabAt(index)?.customView
            updateBottomTabItemColors(inactiveView, false, getDeselectedTabDrawableIds()[index])
            applyTabColorOverride(inactiveView, isActive = false)
        }

        binding.mainTabsHolder.getTabAt(binding.viewPager.currentItem)?.select()
        val bottomBarColor = getBottomNavigationBackgroundColor()
        binding.mainTabsHolder.setBackgroundColor(bottomBarColor)
    }

    private fun getInactiveTabIndexes(activeIndex: Int): List<Int> {
        return arrayListOf(0, 1, 2, 3).filter { it != activeIndex }
    }

    private fun getDeselectedTabDrawableIds() = arrayOf(
        org.fossify.commons.R.drawable.ic_clock_vector,
        R.drawable.ic_alarm_vector,
        R.drawable.ic_stopwatch_vector,
        R.drawable.ic_hourglass_vector
    )

    @Deprecated("Remove this method in future releases")
    private fun migrateFirstDayOfWeek() {
        if (config.migrateFirstDayOfWeek) {
            config.migrateFirstDayOfWeek = false
            config.firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek.value
        }
    }

    private fun getTabIndex(tabId: Int): Int {
        return when (tabId) {
            TAB_CLOCK -> TAB_CLOCK_INDEX
            TAB_ALARM -> TAB_ALARM_INDEX
            TAB_STOPWATCH -> TAB_STOPWATCH_INDEX
            TAB_TIMER -> TAB_TIMER_INDEX
            else -> config.lastUsedViewPagerPage
        }
    }
}
