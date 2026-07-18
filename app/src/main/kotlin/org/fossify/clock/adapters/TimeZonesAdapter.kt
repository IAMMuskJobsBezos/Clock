package org.fossify.clock.adapters

import android.annotation.SuppressLint
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import org.fossify.clock.activities.SimpleActivity
import org.fossify.clock.databinding.ItemTimeZoneBinding
import org.fossify.clock.extensions.getFormattedTime
import org.fossify.clock.models.MyTimeZone
import org.fossify.commons.adapters.MyRecyclerViewAdapter
import org.fossify.commons.extensions.applyColorFilter
import org.fossify.commons.views.MyRecyclerView
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

// Plain non-selectable, non-draggable list - tapping a row opens AddCityActivity scrolled to
// that city (see ClockFragment) instead of a popup dialog or edit dialog; there is no
// long-press delete anymore, that happens by unchecking the city in AddCityActivity itself.
class TimeZonesAdapter(activity: SimpleActivity, var timeZones: ArrayList<MyTimeZone>, recyclerView: MyRecyclerView, itemClick: (Any) -> Unit) :
    MyRecyclerViewAdapter(activity, recyclerView, itemClick) {

    override fun getActionMenuId() = 0

    override fun prepareActionMode(menu: Menu) {}

    override fun actionItemPressed(id: Int) {}

    override fun getSelectableItemCount() = timeZones.size

    override fun getIsItemSelectable(position: Int) = false

    override fun getItemSelectionKey(position: Int) = timeZones.getOrNull(position)?.id

    override fun getItemKeyPosition(key: Int) = timeZones.indexOfFirst { it.id == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return createViewHolder(ItemTimeZoneBinding.inflate(layoutInflater, parent, false).root)
    }

    override fun onBindViewHolder(holder: MyRecyclerViewAdapter.ViewHolder, position: Int) {
        val timeZone = timeZones[position]
        // (true, true) preserves single-tap -> itemClick, same as before this round's rework;
        // long-press selection is blocked separately via getIsItemSelectable() = false above,
        // not by these flags (StopwatchAdapter's (false, false) is for a fully passive list
        // with no tap action at all, which doesn't apply here).
        holder.bindView(timeZone, true, true) { itemView, layoutPosition ->
            setupView(itemView, timeZone)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = timeZones.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateItems(newItems: ArrayList<MyTimeZone>) {
        timeZones = newItems
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateTimes() {
        notifyDataSetChanged()
    }

    private fun setupView(view: View, timeZone: MyTimeZone) {
        val currTimeZone = TimeZone.getTimeZone(timeZone.zoneName)
        val calendar = Calendar.getInstance(currTimeZone)
        var offset = calendar.timeZone.rawOffset
        val isDaylightSavingActive = currTimeZone.inDaylightTime(Date())
        if (isDaylightSavingActive) {
            offset += currTimeZone.dstSavings
        }
        val passedSeconds = ((calendar.timeInMillis + offset) / 1000).toInt()
        // Uppercased to match the AM/PM case shown by the main clock (MyTextClock).
        val formattedTime = activity.getFormattedTime(passedSeconds, false, false).toString().uppercase()

        val localOffsetMillis = Calendar.getInstance().get(Calendar.ZONE_OFFSET) + Calendar.getInstance().get(Calendar.DST_OFFSET)
        val relativeOffsetHours = Math.round((offset - localOffsetMillis) / 3_600_000f)
        val offsetText = if (relativeOffsetHours == 0) {
            "+0hr"
        } else {
            "%+dhr".format(relativeOffsetHours)
        }

        ItemTimeZoneBinding.bind(view).apply {
            timeZoneEditIcon.applyColorFilter(textColor)
            timeZoneTitle.text = timeZone.title
            timeZoneTitle.setTextColor(textColor)

            timeZoneTime.text = formattedTime
            timeZoneTime.setTextColor(textColor)

            timeZoneOffset.text = offsetText
            timeZoneOffset.setTextColor(textColor)
        }
    }
}
