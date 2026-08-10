package org.fossify.clock.extensions

import android.content.res.ColorStateList
import android.graphics.Color
import androidx.core.content.ContextCompat
import org.fossify.clock.R
import org.fossify.commons.views.MyMaterialSwitch

/**
 * MyMaterialSwitch.setColors() always alpha-fades the unchecked-state color to 20-40% opacity
 * internally (it's a thin wrapper over Material3's MaterialSwitch) - that reads as "grayed
 * out," not the crisp look docs/elderly-spec/design-tokens.md specs: filled solid accent with a
 * white knob when on, transparent with a gray (ghostBorder) outline and a pale-plum knob when
 * off. Bypassing setColors() and driving the track/border/thumb tints directly gives full-opacity
 * control over both states instead.
 */
fun styleToggleSwitch(switch: MyMaterialSwitch) {
    val context = switch.context
    val checkedState = intArrayOf(android.R.attr.state_checked)
    val uncheckedState = intArrayOf(-android.R.attr.state_checked)
    val states = arrayOf(checkedState, uncheckedState)

    val accent = ContextCompat.getColor(context, R.color.eb_accent)
    val knobOn = ContextCompat.getColor(context, R.color.eb_knob_on)
    val knobOff = ContextCompat.getColor(context, R.color.eb_toggle_knob_off)
    val ghostBorder = ContextCompat.getColor(context, R.color.eb_ghost_border)

    switch.trackTintList = ColorStateList(states, intArrayOf(accent, Color.TRANSPARENT))
    switch.trackDecorationTintList = ColorStateList(states, intArrayOf(accent, ghostBorder))
    switch.thumbTintList = ColorStateList(states, intArrayOf(knobOn, knobOff))
}
