package org.fossify.clock.extensions

import android.content.res.ColorStateList
import org.fossify.commons.extensions.adjustAlpha
import org.fossify.commons.views.MyMaterialSwitch

/**
 * MyMaterialSwitch.setColors() always alpha-fades the unchecked-state color to 20-40% opacity
 * internally (it's a thin wrapper over Material3's MaterialSwitch) - that reads as "grayed
 * out," not the crisp outlined-purple-when-off / filled-purple-when-on look every toggle in
 * this app is supposed to share. Bypassing setColors() and driving the track/thumb tints
 * directly gives full-opacity control over both states instead.
 */
fun styleToggleSwitch(switch: MyMaterialSwitch, accentColor: Int, offTrackColor: Int) {
    val checkedState = intArrayOf(android.R.attr.state_checked)
    val uncheckedState = intArrayOf(-android.R.attr.state_checked)
    val states = arrayOf(checkedState, uncheckedState)

    val offAccentColor = accentColor.adjustAlpha(0.85f)

    switch.trackTintList = ColorStateList(states, intArrayOf(accentColor, offTrackColor))
    switch.trackDecorationTintList = ColorStateList(states, intArrayOf(accentColor, offAccentColor))
    switch.thumbTintList = ColorStateList(states, intArrayOf(offTrackColor, offAccentColor))
}
