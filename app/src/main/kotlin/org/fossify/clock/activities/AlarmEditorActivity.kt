package org.fossify.clock.activities

import android.os.Bundle
import android.widget.TextView
import org.fossify.clock.R
import org.fossify.clock.databinding.ActivityEditAlarmBinding
import org.fossify.clock.extensions.alarmController
import org.fossify.clock.extensions.cancelAlarmClock
import org.fossify.clock.extensions.config
import org.fossify.clock.extensions.createNewAlarm
import org.fossify.clock.extensions.dbHelper
import org.fossify.clock.extensions.handleFullScreenNotificationsPermission
import org.fossify.clock.extensions.rotateWeekdays
import org.fossify.clock.extensions.styleToggleSwitch
import org.fossify.clock.extensions.updateWidgets
import org.fossify.clock.helpers.DEFAULT_ALARM_MINUTES
import org.fossify.clock.helpers.getTomorrowBit
import org.fossify.clock.helpers.updateNonRecurringAlarmDay
import org.fossify.clock.models.Alarm
import org.fossify.clock.models.AlarmEvent
import org.fossify.commons.dialogs.ConfirmationDialog
import org.fossify.commons.extensions.addBit
import org.fossify.commons.extensions.applyColorFilter
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.removeBit
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.extensions.viewBinding
import org.greenrobot.eventbus.EventBus
import kotlinx.serialization.json.Json

/**
 * Full-screen replacement for EditAlarmDialog, see docs/elderly-spec/alarm.md.
 * Handles Add (no extras), Edit (ALARM_ID extra) and prefilled-Add (PREFILLED_ALARM_JSON
 * extra, used by IntentHandlerActivity for the system ACTION_SET_ALARM intent) flows.
 */
class AlarmEditorActivity : SimpleActivity() {
    companion object {
        const val ALARM_ID = "alarm_id"
        const val PREFILLED_ALARM_JSON = "prefilled_alarm_json"
    }

    private val binding: ActivityEditAlarmBinding by viewBinding(ActivityEditAlarmBinding::inflate)
    private lateinit var alarm: Alarm
    private var isNewAlarm = true
    private val textColor by lazy { getProperTextColor() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupEdgeToEdge(padBottomSystem = listOf(binding.editAlarmButtonsHolder))

        loadAlarm()
        updateTextColors(binding.editAlarmHolder)
        setupTimeWheels()
        setupRepeat()
        setupBottomButtons()
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.editAlarmAppbar)
    }

    private fun loadAlarm() {
        val alarmId = intent.getIntExtra(ALARM_ID, 0)
        val prefilledJson = intent.getStringExtra(PREFILLED_ALARM_JSON)
        val existing = if (alarmId != 0) dbHelper.getAlarmWithId(alarmId) else null
        when {
            existing != null -> {
                alarm = existing
                isNewAlarm = false
            }

            prefilledJson != null -> {
                alarm = Json.decodeFromString(Alarm.serializer(), prefilledJson)
                alarm.id = 0
                isNewAlarm = true
            }

            else -> {
                alarm = createNewAlarm(DEFAULT_ALARM_MINUTES, 0)
                alarm.isEnabled = true
                alarm.days = getTomorrowBit()
                config.alarmLastConfig?.let { lastConfig ->
                    alarm.label = lastConfig.label
                    alarm.days = lastConfig.days
                    alarm.soundTitle = lastConfig.soundTitle
                    alarm.soundUri = lastConfig.soundUri
                    alarm.timeInMinutes = lastConfig.timeInMinutes
                    alarm.vibrate = lastConfig.vibrate
                }
                isNewAlarm = true
            }
        }
    }

    private fun setupTimeWheels() {
        val use24Hour = config.use24HourFormat
        val hours24 = alarm.timeInMinutes / 60
        val minutes = alarm.timeInMinutes % 60

        binding.apply {
            arrayOf(editAlarmHourPicker, editAlarmMinutePicker, editAlarmAmpmPicker).forEach {
                it.textColor = textColor
                it.selectedTextColor = textColor
                it.dividerColor = textColor
            }

            editAlarmMinutePicker.value = minutes
            editAlarmMinutePicker.setOnValueChangedListener { _, _, _ -> onTimeWheelChanged() }

            if (use24Hour) {
                editAlarmAmpmLabel.beVisibleIf(false)
                editAlarmAmpmPicker.beVisibleIf(false)
                editAlarmHourPicker.minValue = 0
                editAlarmHourPicker.maxValue = 23
                editAlarmHourPicker.value = hours24
            } else {
                editAlarmAmpmPicker.displayedValues = resources.getStringArray(R.array.am_pm_values)
                val hour12 = if (hours24 % 12 == 0) 12 else hours24 % 12
                val ampmIndex = if (hours24 < 12) 0 else 1
                editAlarmHourPicker.minValue = 1
                editAlarmHourPicker.maxValue = 12
                editAlarmHourPicker.value = hour12
                editAlarmAmpmPicker.value = ampmIndex
                editAlarmAmpmPicker.setOnValueChangedListener { _, _, _ -> onTimeWheelChanged() }
            }

            editAlarmHourPicker.setOnValueChangedListener { _, _, _ -> onTimeWheelChanged() }
        }
    }

    private fun onTimeWheelChanged() {
        binding.apply {
            val minutes = editAlarmMinutePicker.value
            val hours24 = if (config.use24HourFormat) {
                editAlarmHourPicker.value
            } else {
                val hour12 = editAlarmHourPicker.value
                val isPm = editAlarmAmpmPicker.value == 1
                when {
                    hour12 == 12 && !isPm -> 0
                    hour12 == 12 && isPm -> 12
                    isPm -> hour12 + 12
                    else -> hour12
                }
            }
            alarm.timeInMinutes = hours24 * 60 + minutes
        }
    }

    private fun setupRepeat() {
        binding.apply {
            editAlarmRepeatOffLabel.setTextColor(textColor)
            editAlarmRepeatOnLabel.setTextColor(textColor)
            styleToggleSwitch(editAlarmRepeatSwitch, getProperPrimaryColor(), getProperBackgroundColor())

            val isRecurring = alarm.isRecurring()
            editAlarmRepeatSwitch.isChecked = isRecurring
            editAlarmDaysHolder.beVisibleIf(isRecurring)
            editAlarmDaysHint.beVisibleIf(isRecurring)

            setupDayChips()

            editAlarmRepeatSwitch.setOnCheckedChangeListener { _, isChecked ->
                editAlarmDaysHolder.beVisibleIf(isChecked)
                editAlarmDaysHint.beVisibleIf(isChecked)
                if (!isChecked) {
                    alarm.days = 0
                }
            }
        }
    }

    private fun setupDayChips() {
        binding.editAlarmDaysHolder.removeAllViews()
        val dayLetters = resources.getStringArray(org.fossify.commons.R.array.week_day_letters)
        val dayIndexes = rotateWeekdays(arrayListOf(0, 1, 2, 3, 4, 5, 6))

        dayIndexes.forEach { dayIndex ->
            val bitmask = 1 shl dayIndex
            val chip = layoutInflater.inflate(
                R.layout.alarm_day, binding.editAlarmDaysHolder, false
            ) as TextView
            chip.text = dayLetters[dayIndex]
            val isChecked = alarm.isRecurring() && alarm.days and bitmask != 0
            updateDayChipAppearance(chip, isChecked)
            chip.setOnClickListener {
                // alarm.days may still hold a one-shot sentinel (TODAY_BIT/TOMORROW_BIT, both
                // negative) inherited from config.alarmLastConfig; those aren't real weekday
                // bitmasks, so start a clean slate before the first real day selection.
                if (!alarm.isRecurring()) {
                    alarm.days = 0
                }

                val nowChecked = alarm.days and bitmask == 0
                alarm.days = if (nowChecked) alarm.days.addBit(bitmask) else alarm.days.removeBit(bitmask)
                updateDayChipAppearance(chip, nowChecked)
            }
            binding.editAlarmDaysHolder.addView(chip)
        }
    }

    private fun updateDayChipAppearance(chip: TextView, isSelected: Boolean) {
        val drawableId = if (isSelected) {
            R.drawable.rounded_rect_background_filled
        } else {
            R.drawable.rounded_rect_background_stroke
        }

        val chipColor = getProperPrimaryColor()
        val drawable = resources.getDrawable(drawableId, theme).mutate()
        drawable.applyColorFilter(chipColor)
        chip.background = drawable
        chip.setTextColor(if (isSelected) getProperBackgroundColor() else chipColor)
    }

    private fun deleteAlarmAndFinish() {
        dbHelper.deleteAlarms(arrayListOf(alarm))
        cancelAlarmClock(alarm)
        updateWidgets()
        EventBus.getDefault().post(AlarmEvent.Refresh)
        finish()
    }

    // For an existing alarm, the Cancel slot becomes Delete instead - the back arrow in the
    // app bar is the way to leave without saving, so this slot is free to be repurposed rather
    // than duplicating a second delete button elsewhere on the screen.
    private fun setupBottomButtons() {
        if (isNewAlarm) {
            binding.editAlarmCancel.text = getString(org.fossify.commons.R.string.cancel)
            binding.editAlarmCancel.icon = getDrawable(org.fossify.commons.R.drawable.ic_cross_vector)
            binding.editAlarmCancel.setOnClickListener { finish() }
        } else {
            binding.editAlarmCancel.text = getString(org.fossify.commons.R.string.delete)
            binding.editAlarmCancel.icon = getDrawable(org.fossify.commons.R.drawable.ic_delete_vector)
            binding.editAlarmCancel.setOnClickListener { deleteAlarmAndFinish() }
        }

        binding.editAlarmSave.setOnClickListener { attemptSave() }
    }

    private fun attemptSave() {
        if (binding.editAlarmRepeatSwitch.isChecked && alarm.days <= 0) {
            toast(R.string.no_days_selected)
            return
        }

        if (!config.wasAlarmWarningShown) {
            ConfirmationDialog(
                activity = this,
                messageId = org.fossify.commons.R.string.alarm_warning,
                positive = org.fossify.commons.R.string.ok,
                negative = 0
            ) {
                config.wasAlarmWarningShown = true
                saveAlarm()
            }
        } else {
            saveAlarm()
        }
    }

    private fun saveAlarm() {
        if (!binding.editAlarmRepeatSwitch.isChecked) {
            alarm.days = 0
        }
        updateNonRecurringAlarmDay(alarm)
        alarm.isEnabled = true
        alarm.oneShot = false

        handleFullScreenNotificationsPermission { granted ->
            if (!granted) {
                return@handleFullScreenNotificationsPermission
            }

            if (alarm.id == 0) {
                val newId = dbHelper.insertAlarm(alarm)
                if (newId == -1) {
                    toast(org.fossify.commons.R.string.unknown_error_occurred)
                    return@handleFullScreenNotificationsPermission
                }
                alarm.id = newId
            } else {
                if (!dbHelper.updateAlarm(alarm)) {
                    toast(org.fossify.commons.R.string.unknown_error_occurred)
                    return@handleFullScreenNotificationsPermission
                }
            }

            config.alarmLastConfig = alarm
            alarmController.scheduleNextOccurrence(alarm = alarm, showToasts = true)
            updateWidgets()
            EventBus.getDefault().post(AlarmEvent.Refresh)
            finish()
        }
    }
}
