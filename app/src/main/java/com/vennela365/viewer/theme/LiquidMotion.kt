package com.vennela365.viewer.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Motion language for the liquid-glass interface.
 *
 * Two families coexist on purpose:
 *  - **Liquid** springs for anything the finger touches. They overshoot very
 *    slightly and settle fast, which is what makes surfaces feel wet and alive.
 *  - **Glide** tweens for content entering the viewport, where an overshoot
 *    would read as sloppy rather than playful.
 */
object LiquidMotion {

    // ---- Easing curves -------------------------------------------------

    /** Standard entrance. Decelerates hard, like a dropped droplet spreading. */
    val glideEasing: Easing = CubicBezierEasing(0.16f, 0.84f, 0.24f, 1f)

    /** Exit. Accelerates gently so dismissals never feel yanked away. */
    val exitEasing: Easing = CubicBezierEasing(0.4f, 0f, 0.7f, 0.2f)

    /** Emphasis for hero elements and shared-element style transitions. */
    val emphasisEasing: Easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

    // ---- Durations -----------------------------------------------------

    const val durationInstant = 90
    const val durationFast = 180
    const val durationBase = 280
    const val durationSlow = 420
    const val durationHero = 620

    // ---- Liquid springs ------------------------------------------------

    /** Touch feedback. Snappy, minimal overshoot. */
    fun <T> press(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.62f,
        stiffness = Spring.StiffnessHigh
    )

    /** Surface that grows/settles, e.g. a sheet or an expanding card. */
    fun <T> liquid(): SpringSpec<T> = spring(
        dampingRatio = 0.78f,
        stiffness = Spring.StiffnessMediumLow
    )

    /** Bouncier sibling for celebratory, low-frequency moves. */
    fun <T> playful(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )

    /** Screen and dock transitions: smooth with a whisper of overshoot. */
    fun <T> shared(): SpringSpec<T> = spring(
        dampingRatio = 0.86f,
        stiffness = Spring.StiffnessMediumLow
    )

    // ---- Glide tweens --------------------------------------------------

    fun <T> glide(duration: Int = durationBase, delay: Int = 0): TweenSpec<T> =
        tween(durationMillis = duration, delayMillis = delay, easing = glideEasing)

    fun <T> exit(duration: Int = durationFast): TweenSpec<T> =
        tween(durationMillis = duration, easing = exitEasing)

    fun <T> emphasis(duration: Int = durationSlow): TweenSpec<T> =
        tween(durationMillis = duration, easing = emphasisEasing)

    // ---- Aurora upgrade additions ---------------------------------------

    /** Hero surfaces: slower, airier, with a generous overshoot. */
    fun <T> hero(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = 240f
    )

    /** Celebratory bounce for rewards and "wow" moments. */
    fun <T> bounce(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessVeryLow
    )

    /** Sheen sweeps and shimmer loops. */
    fun <T> shimmer(duration: Int = durationHero): TweenSpec<T> =
        tween(durationMillis = duration, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f))

    /**
     * Staggered list cascade: one spec per index so rows ripple into view.
     * The delay is clamped so long lists still settle quickly.
     */
    fun <T> cascade(
        index: Int,
        stepMillis: Int = 46,
        duration: Int = durationBase
    ): TweenSpec<T> = tween(
        durationMillis = duration,
        delayMillis = (index.coerceAtMost(10) * stepMillis),
        easing = glideEasing
    )
}
