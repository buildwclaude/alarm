package com.buildwclaude.alarm.ui.neu

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** A soft-UI toggle: a recessed track with a raised knob that turns the accent colour on. */
@Composable
fun NeuSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trackW = 66.dp
    val trackH = 34.dp
    val knob = 26.dp
    val pad = 4.dp

    val knobOffset by animateDpAsState(
        if (checked) trackW - knob - pad else pad,
        tween(220),
        label = "knob",
    )
    val knobColor by animateColorAsState(
        if (checked) Neu.Accent else Neu.Surface,
        tween(220),
        label = "knobColor",
    )

    Box(
        modifier
            .size(trackW, trackH)
            .neuInset(cornerRadius = trackH / 2)
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                role = Role.Switch,
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset(x = knobOffset)
                .size(knob)
                .neuRaised(cornerRadius = knob / 2, surface = knobColor, offset = 3.dp, blur = 6.dp),
        )
    }
}
