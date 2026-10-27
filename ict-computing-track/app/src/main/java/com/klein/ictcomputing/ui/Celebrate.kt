package com.klein.ictcomputing.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ============================ Haptics ============================ */

/** Petites vibrations « gameplay » : bon, mauvais, tap, boss down, level up. */
object Haptics {
    enum class Kind { GOOD, BAD, TAP, BOSS_DOWN, LEVEL_UP }

    fun fire(view: android.view.View?, kind: Kind) {
        val v = view ?: return
        when (kind) {
            Kind.GOOD -> v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            Kind.TAP -> v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            Kind.BAD -> v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            Kind.BOSS_DOWN -> repeat(3) { v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY) }
            Kind.LEVEL_UP -> repeat(2) { v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) }
        }
    }
}

/* ============================ Confetti ============================ */

private class ConfettiParticle {
    var x = 0f
    var y = 0f
    var vx = 0f
    var vy = 0f
    var rot = 0f
    var vr = 0f
    var color = Color.White
    var size = 8f
}

@Composable
fun ConfettiOverlay(modifier: Modifier = Modifier, trigger: Int, burst: Int = 80) {
    if (trigger <= 0) return
    val colors = remember {
        listOf(
            ContestColors.AccentRed, ContestColors.AccentCyan, ContestColors.AccentGreen,
            ContestColors.AccentAmber, ContestColors.AccentPurple, Color.White,
        )
    }
    val parts = remember(trigger) {
        val r = kotlin.random.Random(trigger)
        (0 until burst).map {
            ConfettiParticle().apply {
                x = 0.5f + (r.nextFloat() - 0.5f) * 0.4f
                y = 0.45f
                vx = (r.nextFloat() - 0.5f) * 0.8f
                vy = -0.9f - r.nextFloat() * 0.8f
                vr = (r.nextFloat() - 0.5f) * 40f
                rot = r.nextFloat() * 360f
                color = colors[r.nextInt(colors.size)]
                size = 6f + r.nextFloat() * 9f
            }
        }
    }
    val elapsed = remember(trigger) { Animatable(0f) }
    LaunchedEffect(trigger) {
        elapsed.snapTo(0f)
        elapsed.animateTo(1f, animationSpec = tween(2600, easing = LinearEasing))
    }
    val t = elapsed.value
    if (t >= 1f) return
    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val alpha = (1f - t).coerceIn(0f, 1f)
        parts.forEach { p ->
            val px = ((p.x + p.vx * t * 1.4f) * w)
            val py = ((p.y + p.vy * t + 1.8f * t * t) * h)
            val s = (p.size * 1.4f).dp.toPx()
            val paint = Stroke(width = 3f)
            rotate(degrees = p.rot + t * p.vr, pivot = Offset(px, py)) {
                drawRect(
                    color = p.color.copy(alpha = alpha),
                    topLeft = Offset(px - s / 2, py - s / 2),
                    size = Size(s, s),
                    style = paint,
                )
            }
        }
    }
}

/* ============================ XP flottant ============================ */

/** « +X XP » qui s'envole puis disparaît — à poser après une réussite. */
@Composable
fun XpPop(amount: Int, seed: Int, big: Boolean = false, color: Color = ContestColors.AccentGreen) {
    if (amount <= 0) return
    val view = LocalView.current
    val rise = remember(seed) { Animatable(0f) }
    val fade = remember(seed) { Animatable(1f) }
    LaunchedEffect(seed) {
        Haptics.fire(view, if (big) Haptics.Kind.LEVEL_UP else Haptics.Kind.GOOD)
        rise.snapTo(0f)
        rise.animateTo(1f, animationSpec = tween(750, easing = LinearEasing))
        fade.animateTo(0f, animationSpec = tween(220))
    }
    val stepwise = if (rise.value >= 1f) 1f else rise.value
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            (if (big) "+$amount XP 🏆" else "+$amount XP ⚡"),
            color = color.copy(alpha = fade.value),
            fontSize = if (big) 18.sp else 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.offset(y = (-(stepwise * 96)).toInt().dp),
        )
    }
}
