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

    const val durationInstant = 80
    const val durationFast = 160
    const val durationBase = 240
    const val durationSlow = 360
    const val durationHero = 520

    // ---- Liquid springs ------------------------------------------------

    /** Touch feedback: crisp, immediate response with subtle elastic settle. */
    fun <T> press(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.68f,
        stiffness = Spring.StiffnessHigh
    )

    /** Surface that grows/settles, e.g. a sheet or expanding card. */
    fun <T> liquid(): SpringSpec<T> = spring(
        dampingRatio = 0.82f,
        stiffness = Spring.StiffnessMedium
    )

    /** Bouncier sibling for celebratory, low-frequency moves. */
    fun <T> playful(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    /** Screen and dock transitions: ultra-smooth critically damped glide. */
    fun <T> shared(): SpringSpec<T> = spring(
        dampingRatio = 0.88f,
        stiffness = 460f
    )

    // ---- Glide tweens --------------------------------------------------

    fun <T> glide(duration: Int = durationBase, delay: Int = 0): TweenSpec<T> =
        tween(durationMillis = duration, delayMillis = delay, easing = glideEasing)

    fun <T> exit(duration: Int = durationFast): TweenSpec<T> =
        tween(durationMillis = duration, easing = exitEasing)

    fun <T> emphasis(duration: Int = durationSlow): TweenSpec<T> =
        tween(durationMillis = duration, easing = emphasisEasing)

    // ---- Aurora upgrade additions ---------------------------------------

    /** Hero surfaces: smooth, balanced overshoot. */
    fun <T> hero(): SpringSpec<T> = spring(
        dampingRatio = 0.72f,
        stiffness = 380f
    )

    /** Celebratory bounce for rewards and highlights. */
    fun <T> bounce(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    /** Sheen sweeps and shimmer loops. */
    fun <T> shimmer(duration: Int = durationHero): TweenSpec<T> =
        tween(durationMillis = duration, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f))

    /**
     * Staggered list cascade: crisp tight stagger so lists snap into focus immediately.
     */
    fun <T> cascade(
        index: Int,
        stepMillis: Int = 30,
        duration: Int = durationBase
    ): TweenSpec<T> = tween(
        durationMillis = duration,
        delayMillis = (index.coerceAtMost(8) * stepMillis),
        easing = glideEasing
    )
}
