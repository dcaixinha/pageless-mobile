package live.pageless.mobile.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Themed pull-to-refresh indicator for Pageless: a ring of short radial line
 * segments in the brand purple that rotate while their length pulses in and out,
 * housed in a rounded surface frame.
 *
 * While the user drags (before a refresh is triggered) the indicator fades and
 * scales in with a wind-up rotation proportional to the pull distance; once
 * refreshing it spins and pulses continuously.
 *
 * Mirrors the framed spinner look used elsewhere in the app; drop it into a
 * [androidx.compose.material3.pulltorefresh.PullToRefreshBox]'s `indicator` slot.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PagelessRefreshIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
) {
    val segments = 10
    val color = MaterialTheme.colorScheme.primary
    val frameColor = MaterialTheme.colorScheme.surfaceVariant

    // Pull progress clamped to [0, 1]; drives fade/scale/wind-up while dragging.
    val pull = state.distanceFraction.coerceIn(0f, 1f)
    val active = isRefreshing || pull > 0f

    val transition = rememberInfiniteTransition(label = "refresh")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = LinearEasing),
            ),
        label = "spin",
    )
    // Pulse phase (0..2π) modulating each segment's length as it spins.
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "pulse",
    )

    // While dragging, rotation "winds up" with the pull; while refreshing it spins.
    val rotation = if (isRefreshing) spin else pull * 180f
    val appearance = if (isRefreshing) 1f else pull

    Box(
        modifier =
            modifier
                .graphicsLayer {
                    alpha = appearance
                    scaleX = 0.6f + 0.4f * appearance
                    scaleY = 0.6f + 0.4f * appearance
                }.shadow(6.dp, RoundedCornerShape(16.dp), clip = false)
                .size(44.dp)
                .background(frameColor, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier.size(26.dp),
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerMax = size.minDimension / 2f
            val innerRadius = outerMax * 0.42f
            val strokeWidth = outerMax * 0.16f
            val rotationRad = (rotation * PI / 180f).toFloat()

            for (i in 0 until segments) {
                val fraction = i.toFloat() / segments
                val angle = rotationRad + fraction * (2 * PI).toFloat()

                // Length pulses per-segment via a phase offset around the ring.
                val phase = pulse + fraction * (2 * PI).toFloat()
                val lengthScale = 0.55f + 0.45f * ((sin(phase.toDouble()).toFloat() + 1f) / 2f)
                val outerRadius = innerRadius + (outerMax - innerRadius) * lengthScale

                // Trailing segments fade for a comet-like read.
                val alpha =
                    if (active && !isRefreshing) {
                        1f
                    } else {
                        0.25f + 0.75f * fraction
                    }

                val cosA = cos(angle.toDouble()).toFloat()
                val sinA = sin(angle.toDouble()).toFloat()
                val start =
                    Offset(
                        center.x + cosA * innerRadius,
                        center.y + sinA * innerRadius,
                    )
                val end =
                    Offset(
                        center.x + cosA * outerRadius,
                        center.y + sinA * outerRadius,
                    )

                drawLine(
                    color = color.copy(alpha = alpha),
                    start = start,
                    end = end,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}
