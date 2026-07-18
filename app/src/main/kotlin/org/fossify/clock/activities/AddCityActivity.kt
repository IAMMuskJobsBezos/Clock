package org.fossify.clock.activities

import android.os.Bundle
import androidx.core.widget.doAfterTextChanged
import org.fossify.clock.R
import org.fossify.clock.adapters.SelectTimeZonesAdapter
import org.fossify.clock.databinding.ActivityAddCityBinding
import org.fossify.clock.extensions.config
import org.fossify.clock.helpers.getAllTimeZones
import org.fossify.commons.extensions.adjustAlpha
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.extensions.viewBinding

/**
 * Full-screen replacement for AddTimeZonesDialog, see docs/elderly-spec/clock.md.
 */
class AddCityActivity : SimpleActivity() {
    companion object {
        // Optional MyTimeZone.id - when present, scrolls the list to that city on open so a
        // tap on a Clock-tab world-clock row lands the user directly on it instead of a
        // generic empty search screen.
        const val SCROLL_TO_TIME_ZONE_ID = "scroll_to_time_zone_id"
    }

    private val binding: ActivityAddCityBinding by viewBinding(ActivityAddCityBinding::inflate)
    private val allTimeZones = getAllTimeZones()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupEdgeToEdge(padBottomSystem = listOf(binding.addCityButtonsHolder))
        updateTextColors(binding.addCityHolder)
        // updateTextColors() sweeps every child and calls MyEditText.setColors() on this field,
        // which color-filters whatever background drawable is set to the primary color - that
        // clobbers the plain grey search_field_background. Reapply it fresh (no filter) after.
        binding.addCitySearch.background = getDrawable(R.drawable.search_field_background)
        binding.addCitySearch.setHintTextColor(getProperTextColor().adjustAlpha(0.6f))

        binding.addCityList.adapter = SelectTimeZonesAdapter(this, allTimeZones)
        scrollToRequestedTimeZone()

        binding.addCitySearch.doAfterTextChanged { query ->
            filterCities(query?.toString().orEmpty())
        }

        binding.addCityCancel.setOnClickListener {
            finish()
        }

        binding.addCitySave.setOnClickListener {
            saveAndFinish()
        }
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.addCityAppbar)
    }

    private fun scrollToRequestedTimeZone() {
        val targetId = intent.getIntExtra(SCROLL_TO_TIME_ZONE_ID, -1)
        if (targetId == -1) {
            return
        }

        val position = allTimeZones.indexOfFirst { it.id == targetId }
        if (position != -1) {
            binding.addCityList.scrollToPosition(position)
        }
    }

    private fun filterCities(query: String) {
        val adapter = binding.addCityList.adapter as? SelectTimeZonesAdapter ?: return
        val filtered = if (query.isBlank()) {
            allTimeZones
        } else {
            ArrayList(allTimeZones.filter { it.title.contains(query, ignoreCase = true) })
        }
        adapter.updateItems(filtered)
    }

    private fun saveAndFinish() {
        val adapter = binding.addCityList.adapter as? SelectTimeZonesAdapter
        val selectedTimeZones = adapter?.selectedKeys?.map { it.toString() }?.toHashSet() ?: LinkedHashSet()
        config.selectedTimeZones = selectedTimeZones
        finish()
    }
}
