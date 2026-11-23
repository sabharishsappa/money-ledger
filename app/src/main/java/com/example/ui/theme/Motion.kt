package com.example.ui.theme

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * Centralized high-responsiveness motion system for Money Ledger.
 * Uses Material 3 decelerate curves, tight GPU-friendly durations, and
 * interruptible no-bounce springs for silky-smooth 60/120Hz feel.
 */
object LedgerMotion {
    // Material 3 standard decelerate — snappy initial velocity, smooth landing
    val SnappyDecelerate = CubicBezierEasing(0.18f, 0.0f, 0.0f, 1.0f)
    val QuickAccelerate = CubicBezierEasing(0.3f, 0.0f, 1.0f, 1.0f)

    fun <T> microTween(durationMs: Int = 100): TweenSpec<T> =
        tween(durationMillis = durationMs, easing = SnappyDecelerate)

    fun <T> snappyTween(durationMs: Int = 135): TweenSpec<T> =
        tween(durationMillis = durationMs, easing = SnappyDecelerate)

    fun <T> chartTween(durationMs: Int = 260): TweenSpec<T> =
        tween(durationMillis = durationMs, easing = SnappyDecelerate)

    fun <T> smoothSpring(visibilityThreshold: T? = null): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
        visibilityThreshold = visibilityThreshold
    )

    fun <T> quickSpring(visibilityThreshold: T? = null): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh,
        visibilityThreshold = visibilityThreshold
    )

    fun tabSwitchTransform(forward: Boolean): ContentTransform {
        val dir = if (forward) 1 else -1
        return (slideInHorizontally(
            animationSpec = tween<IntOffset>(125, easing = SnappyDecelerate),
            initialOffsetX = { dir * (it / 24) }
        ) + fadeIn(animationSpec = tween(110, easing = SnappyDecelerate)))
            .togetherWith(
                fadeOut(animationSpec = tween(80, easing = QuickAccelerate))
            )
    }

    fun pushPopTransform(goingBack: Boolean): ContentTransform {
        val dir = if (goingBack) -1 else 1
        return (slideInHorizontally(
            animationSpec = tween<IntOffset>(145, easing = SnappyDecelerate),
            initialOffsetX = { dir * (it / 8) }
        ) + fadeIn(animationSpec = tween(125, easing = SnappyDecelerate)))
            .togetherWith(
                slideOutHorizontally(
                    animationSpec = tween<IntOffset>(115, easing = QuickAccelerate),
                    targetOffsetX = { -dir * (it / 12) }
                ) + fadeOut(animationSpec = tween(95, easing = QuickAccelerate))
            )
    }

    fun overlayTransform(entering: Boolean, exiting: Boolean): ContentTransform = when {
        entering -> {
            (slideInVertically(
                animationSpec = tween<IntOffset>(150, easing = SnappyDecelerate),
                initialOffsetY = { it / 22 }
            ) + fadeIn(animationSpec = tween(130, easing = SnappyDecelerate)))
                .togetherWith(fadeOut(animationSpec = tween(90, easing = QuickAccelerate)))
        }
        exiting -> {
            fadeIn(animationSpec = tween(120, easing = SnappyDecelerate))
                .togetherWith(
                    slideOutVertically(
                        animationSpec = tween<IntOffset>(120, easing = QuickAccelerate),
                        targetOffsetY = { it / 22 }
                    ) + fadeOut(animationSpec = tween(100, easing = QuickAccelerate))
                )
        }
        else -> {
            fadeIn(animationSpec = tween(115, easing = SnappyDecelerate))
                .togetherWith(fadeOut(animationSpec = tween(85, easing = QuickAccelerate)))
        }
    }

    fun fastFadeTransform(): ContentTransform =
        fadeIn(tween(115, easing = SnappyDecelerate)) togetherWith
            fadeOut(tween(85, easing = QuickAccelerate))

    fun expandEnter(): EnterTransition =
        expandVertically(tween<IntSize>(135, easing = SnappyDecelerate)) +
            fadeIn(tween(110, easing = SnappyDecelerate))

    fun shrinkExit(): ExitTransition =
        shrinkVertically(tween<IntSize>(115, easing = QuickAccelerate)) +
            fadeOut(tween(90, easing = QuickAccelerate))
}
