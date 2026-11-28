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

    fun <T> microTween(durationMs: Int = 90): TweenSpec<T> =
        tween(durationMillis = durationMs, easing = SnappyDecelerate)

    fun <T> snappyTween(durationMs: Int = 120): TweenSpec<T> =
        tween(durationMillis = durationMs, easing = SnappyDecelerate)

    fun <T> chartTween(durationMs: Int = 220): TweenSpec<T> =
        tween(durationMillis = durationMs, easing = SnappyDecelerate)

    fun <T> smoothSpring(visibilityThreshold: T? = null): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow * 1.5f,
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
            animationSpec = tween<IntOffset>(115, easing = SnappyDecelerate),
            initialOffsetX = { dir * (it / 26) }
        ) + fadeIn(animationSpec = tween(100, easing = SnappyDecelerate)))
            .togetherWith(
                fadeOut(animationSpec = tween(70, easing = QuickAccelerate))
            )
    }

    fun periodSwitchTransform(forward: Boolean): ContentTransform {
        val dir = if (forward) 1 else -1
        return (slideInHorizontally(
            animationSpec = tween<IntOffset>(130, easing = SnappyDecelerate),
            initialOffsetX = { dir * (it / 10) }
        ) + fadeIn(animationSpec = tween(110, easing = SnappyDecelerate)))
            .togetherWith(
                slideOutHorizontally(
                    animationSpec = tween<IntOffset>(105, easing = QuickAccelerate),
                    targetOffsetX = { -dir * (it / 14) }
                ) + fadeOut(animationSpec = tween(85, easing = QuickAccelerate))
            )
    }

    fun pushPopTransform(goingBack: Boolean): ContentTransform {
        val dir = if (goingBack) -1 else 1
        return (slideInHorizontally(
            animationSpec = tween<IntOffset>(135, easing = SnappyDecelerate),
            initialOffsetX = { dir * (it / 10) }
        ) + fadeIn(animationSpec = tween(115, easing = SnappyDecelerate)))
            .togetherWith(
                slideOutHorizontally(
                    animationSpec = tween<IntOffset>(105, easing = QuickAccelerate),
                    targetOffsetX = { -dir * (it / 14) }
                ) + fadeOut(animationSpec = tween(85, easing = QuickAccelerate))
            )
    }

    fun overlayTransform(entering: Boolean, exiting: Boolean): ContentTransform = when {
        entering -> {
            (slideInVertically(
                animationSpec = tween<IntOffset>(135, easing = SnappyDecelerate),
                initialOffsetY = { it / 26 }
            ) + fadeIn(animationSpec = tween(115, easing = SnappyDecelerate)))
                .togetherWith(fadeOut(animationSpec = tween(80, easing = QuickAccelerate)))
        }
        exiting -> {
            fadeIn(animationSpec = tween(105, easing = SnappyDecelerate))
                .togetherWith(
                    slideOutVertically(
                        animationSpec = tween<IntOffset>(110, easing = QuickAccelerate),
                        targetOffsetY = { it / 26 }
                    ) + fadeOut(animationSpec = tween(90, easing = QuickAccelerate))
                )
        }
        else -> {
            fadeIn(animationSpec = tween(105, easing = SnappyDecelerate))
                .togetherWith(fadeOut(animationSpec = tween(75, easing = QuickAccelerate)))
        }
    }

    fun fastFadeTransform(): ContentTransform =
        fadeIn(tween(105, easing = SnappyDecelerate)) togetherWith
            fadeOut(tween(75, easing = QuickAccelerate))

    fun expandEnter(): EnterTransition =
        expandVertically(tween<IntSize>(120, easing = SnappyDecelerate)) +
            fadeIn(tween(100, easing = SnappyDecelerate))

    fun shrinkExit(): ExitTransition =
        shrinkVertically(tween<IntSize>(105, easing = QuickAccelerate)) +
            fadeOut(tween(80, easing = QuickAccelerate))
}
