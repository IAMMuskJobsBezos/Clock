package org.fossify.clock.adapters

import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import org.fossify.clock.R
import org.fossify.clock.activities.SimpleActivity
import org.fossify.clock.databinding.ItemLapBinding
import org.fossify.clock.extensions.getFormattedDuration
import org.fossify.clock.models.Lap
import org.fossify.commons.adapters.MyRecyclerViewListAdapter
import org.fossify.commons.views.MyRecyclerView

/**
 * Laps are always shown newest-first with no sorting controls (decision #11).
 */
class StopwatchAdapter(
    activity: SimpleActivity,
    recyclerView: MyRecyclerView,
) : MyRecyclerViewListAdapter<Lap>(
    activity = activity,
    recyclerView = recyclerView,
    diffUtil = LapDiffCallback(),
    itemClick = {}
) {

    init {
        setHasStableIds(true)
        recyclerView.itemAnimator = null
    }

    override fun getActionMenuId() = 0

    override fun prepareActionMode(menu: Menu) {}

    override fun actionItemPressed(id: Int) {}

    override fun getSelectableItemCount() = currentList.size

    override fun getIsItemSelectable(position: Int) = false

    override fun getItemSelectionKey(position: Int) = currentList.getOrNull(position)?.id

    override fun getItemKeyPosition(key: Int) = currentList.indexOfFirst { it.id == key }

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    override fun getItemId(position: Int): Long {
        return getItem(position)?.id?.toLong() ?: 0L
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return createViewHolder(ItemLapBinding.inflate(layoutInflater, parent, false).root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val lap = getItem(position)
        holder.bindView(lap, false, false) { itemView, layoutPosition ->
            setupView(itemView, lap)
        }
        bindViewHolder(holder)
    }

    private fun setupView(view: View, lap: Lap) {
        ItemLapBinding.bind(view).apply {
            lapOrder.text = activity.getString(R.string.lap_number_format, lap.id)
            // Sub, not body ink - docs/elderly-spec/design-tokens.md ("Lap N" ... sub).
            lapOrder.setTextColor(ContextCompat.getColor(activity, R.color.eb_sub))

            lapLapTime.text = lap.lapTime.getFormattedDuration(forceShowHours = true)
            lapLapTime.setTextColor(textColor)
        }
    }

    private class LapDiffCallback : DiffUtil.ItemCallback<Lap>() {
        override fun areItemsTheSame(oldItem: Lap, newItem: Lap) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Lap, newItem: Lap) = oldItem == newItem
    }
}
