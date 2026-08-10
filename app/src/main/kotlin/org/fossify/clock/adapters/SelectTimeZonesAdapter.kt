package org.fossify.clock.adapters

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.fossify.clock.activities.SimpleActivity
import org.fossify.clock.databinding.ItemAddTimeZoneBinding
import org.fossify.clock.extensions.config
import org.fossify.clock.R
import org.fossify.clock.models.MyTimeZone
import org.fossify.commons.extensions.applyColorFilter
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.getProperTextColor
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

class SelectTimeZonesAdapter(val activity: SimpleActivity, var timeZones: ArrayList<MyTimeZone>) : RecyclerView.Adapter<SelectTimeZonesAdapter.ViewHolder>() {
    private val config = activity.config
    private val textColor = activity.getProperTextColor()
    private val primaryColor = activity.getProperPrimaryColor()
    var selectedKeys = HashSet<Int>()

    init {
        val selectedTimeZones = config.selectedTimeZones
        timeZones.forEachIndexed { index, myTimeZone ->
            if (selectedTimeZones.contains(myTimeZone.id.toString())) {
                selectedKeys.add(myTimeZone.id)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateItems(newItems: ArrayList<MyTimeZone>) {
        timeZones = newItems
        notifyDataSetChanged()
    }

    private fun toggleItemSelection(select: Boolean, pos: Int) {
        val itemKey = timeZones.getOrNull(pos)?.id ?: return

        if (select) {
            selectedKeys.add(itemKey)
        } else {
            selectedKeys.remove(itemKey)
        }

        notifyItemChanged(pos)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemAddTimeZoneBinding.inflate(activity.layoutInflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindView(timeZones[position], textColor, primaryColor)
    }

    override fun getItemCount() = timeZones.size

    // Relative to the device's local time, matching TimeZonesAdapter (Clock tab world-clock
    // list) and docs/elderly-spec/clock.md decision #14 - e.g. "+3hr", "-8hr", "+0hr".
    private fun getOffsetText(zoneName: String): String {
        val currTimeZone = TimeZone.getTimeZone(zoneName)
        var offsetMs = currTimeZone.rawOffset
        if (currTimeZone.inDaylightTime(Date())) {
            offsetMs += currTimeZone.dstSavings
        }

        val localOffsetMillis = Calendar.getInstance().get(Calendar.ZONE_OFFSET) + Calendar.getInstance().get(Calendar.DST_OFFSET)
        val relativeOffsetHours = Math.round((offsetMs - localOffsetMillis) / 3_600_000f)
        return if (relativeOffsetHours == 0) "+0hr" else "%+dhr".format(relativeOffsetHours)
    }

    private fun updateSelectorAppearance(selector: View, isSelected: Boolean, primaryColor: Int) {
        val drawableId = if (isSelected) R.drawable.circle_selector_filled else R.drawable.circle_selector_stroke
        val drawable = selector.context.resources.getDrawable(drawableId, selector.context.theme).mutate()
        drawable.applyColorFilter(primaryColor)
        selector.background = drawable
    }

    inner class ViewHolder(private val binding: ItemAddTimeZoneBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindView(myTimeZone: MyTimeZone, textColor: Int, primaryColor: Int): View {
            val isSelected = selectedKeys.contains(myTimeZone.id)
            binding.apply {
                updateSelectorAppearance(addTimeZoneSelector, isSelected, primaryColor)
                addTimeZoneTitle.text = myTimeZone.title
                addTimeZoneTitle.setTextColor(textColor)
                addTimeZoneOffset.text = getOffsetText(myTimeZone.zoneName)
                // Accent, not body ink - see docs/elderly-spec/design-tokens.md ("Offset:
                // Poppins 700, 20px, accent").
                addTimeZoneOffset.setTextColor(primaryColor)

                addTimeZoneHolder.setOnClickListener {
                    viewClicked(myTimeZone)
                }
            }

            return itemView
        }

        private fun viewClicked(myTimeZone: MyTimeZone) {
            val isSelected = selectedKeys.contains(myTimeZone.id)
            toggleItemSelection(!isSelected, adapterPosition)
        }
    }
}
