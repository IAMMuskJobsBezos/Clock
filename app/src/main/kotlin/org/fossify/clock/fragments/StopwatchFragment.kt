package org.fossify.clock.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fossify.clock.R
import org.fossify.clock.activities.SimpleActivity
import org.fossify.clock.adapters.StopwatchAdapter
import org.fossify.clock.databinding.FragmentStopwatchBinding
import org.fossify.clock.extensions.config
import org.fossify.clock.helpers.Stopwatch
import org.fossify.commons.dialogs.PermissionRequiredDialog
import org.fossify.commons.extensions.beGone
import org.fossify.commons.extensions.beVisible
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.openNotificationSettings
import org.fossify.commons.extensions.updateTextColors

/**
 * Whole seconds only, laps always newest-first, no sorting UI, Lap disabled (not hidden)
 * while paused - see docs/elderly-spec/stopwatch.md and decision #11.
 */
class StopwatchFragment : Fragment() {

    private var stopwatchAdapter: StopwatchAdapter? = null
    private lateinit var binding: FragmentStopwatchBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentStopwatchBinding.inflate(inflater, container, false).apply {
            stopwatchPlayPause.setOnClickListener {
                togglePlayPause()
            }

            stopwatchReset.setOnClickListener {
                resetStopwatch()
            }

            stopwatchLap.setOnClickListener {
                if (Stopwatch.state == Stopwatch.State.RUNNING) {
                    Stopwatch.lap()
                    updateLaps()
                    scrollToTop()
                }
            }

            stopwatchAdapter = StopwatchAdapter(
                activity = activity as SimpleActivity,
                recyclerView = stopwatchList
            )
            stopwatchList.adapter = stopwatchAdapter
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        setupViews()
        Stopwatch.addUpdateListener(updateListener)
        updateLaps()

        if (requireContext().config.toggleStopwatch) {
            requireContext().config.toggleStopwatch = false
            startStopWatch()
        }
    }

    override fun onPause() {
        super.onPause()
        Stopwatch.removeUpdateListener(updateListener)
    }

    private fun setupViews() {
        val properTextColor = requireContext().getProperTextColor()
        binding.apply {
            requireContext().updateTextColors(stopwatchFragment)
        }

        stopwatchAdapter?.apply {
            updatePrimaryColor()
            updateBackgroundColor(requireContext().getProperBackgroundColor())
            updateTextColor(properTextColor)
        }
    }

    private fun togglePlayPause() {
        (activity as SimpleActivity).handleNotificationPermission { granted ->
            if (granted) {
                Stopwatch.toggle()
            } else {
                PermissionRequiredDialog(
                    activity as SimpleActivity,
                    org.fossify.commons.R.string.allow_notifications_reminders,
                    { (activity as SimpleActivity).openNotificationSettings() })
            }
        }
    }

    private fun resetStopwatch() {
        Stopwatch.reset()
        updateLaps()
        updateTimeDisplay(0L)
    }

    private fun updateTimeDisplay(totalTimeMs: Long) {
        val totalSeconds = totalTimeMs / 1000L
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        binding.stopwatchTime.text = "%d:%02d:%02d".format(hours, minutes, seconds)
    }

    fun startStopWatch() {
        if (Stopwatch.state == Stopwatch.State.STOPPED) {
            togglePlayPause()
        }
    }

    private fun updateLaps() = viewLifecycleOwner.lifecycleScope.launch {
        stopwatchAdapter?.submitList(
            withContext(Dispatchers.Default) { ArrayList(Stopwatch.laps) }
        )
        binding.stopwatchList.beVisibleIf(Stopwatch.laps.isNotEmpty())
    }

    private fun scrollToTop() {
        binding.stopwatchList.post {
            binding.stopwatchList.scrollToPosition(0)
        }
    }

    private fun updateButtonsForState(state: Stopwatch.State) {
        binding.apply {
            when (state) {
                Stopwatch.State.STOPPED -> {
                    stopwatchSecondaryButtonsHolder.beGone()
                    stopwatchPlayPause.text = getString(R.string.start_stopwatch_elderly)
                    stopwatchPlayPause.setIconResource(org.fossify.commons.R.drawable.ic_play_vector)
                }

                Stopwatch.State.PAUSED -> {
                    stopwatchSecondaryButtonsHolder.beVisible()
                    stopwatchLap.isEnabled = false
                    stopwatchLap.alpha = 0.4f
                    stopwatchPlayPause.text = getString(R.string.start_stopwatch_elderly)
                    stopwatchPlayPause.setIconResource(org.fossify.commons.R.drawable.ic_play_vector)
                }

                Stopwatch.State.RUNNING -> {
                    stopwatchSecondaryButtonsHolder.beVisible()
                    stopwatchLap.isEnabled = true
                    stopwatchLap.alpha = 1f
                    stopwatchPlayPause.text = getString(R.string.stop_stopwatch)
                    stopwatchPlayPause.setIconResource(org.fossify.commons.R.drawable.ic_pause_vector)
                }
            }
        }
    }

    private val updateListener = object : Stopwatch.UpdateListener {
        override fun onUpdate(totalTime: Long, lapTime: Long, useLongerMSFormat: Boolean) {
            updateTimeDisplay(totalTime)
        }

        override fun onStateChanged(state: Stopwatch.State) {
            updateButtonsForState(state)
        }
    }
}
