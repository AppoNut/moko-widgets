/*
 * Copyright 2019 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.icerock.moko.widgets.utils

import dev.icerock.moko.graphics.toUIColor
import dev.icerock.moko.widgets.objc.cgColors
import dev.icerock.moko.widgets.style.background.Background
import dev.icerock.moko.widgets.style.background.Direction
import dev.icerock.moko.widgets.style.background.Fill
import dev.icerock.moko.widgets.style.background.Shape
import dev.icerock.moko.widgets.style.background.StateBackground
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.QuartzCore.CAGradientLayer
import platform.QuartzCore.CALayer
import platform.QuartzCore.CATransaction
import platform.UIKit.UIButton
import platform.UIKit.UIColor
import platform.UIKit.UIControl
import platform.UIKit.UIView
import platform.UIKit.backgroundColor
import kotlin.math.min

fun Background.caLayer(): CALayer {

    val backgroundLayer: CALayer

    when (fill) {
        is Fill.Solid -> backgroundLayer = CALayer().apply {
            backgroundColor = fill.color.toUIColor().CGColor
        }
        is Fill.Gradient -> {
            backgroundLayer = CAGradientLayer().apply {
                colors = cgColors(fill.colors.map {
                    it.toUIColor()
                })

                masksToBounds = true

                val (start, end) = when (fill.direction) {
                    Direction.LEFT_RIGHT -> CGPointMake(0.0, 0.5) to CGPointMake(1.0, 0.5)
                    Direction.RIGHT_LEFT -> CGPointMake(1.0, 0.5) to CGPointMake(0.0, 0.5)
                    Direction.TOP_BOTTOM -> CGPointMake(0.5, 0.0) to CGPointMake(0.5, 1.0)
                    Direction.BOTTOM_TOP -> CGPointMake(0.5, 1.0) to CGPointMake(0.5, 0.0)
                    Direction.BL_TR -> CGPointMake(0.0, 1.0) to CGPointMake(1.0, 0.0)
                    Direction.BR_TL -> CGPointMake(1.0, 1.0) to CGPointMake(0.0, 0.0)
                    Direction.TR_BL -> CGPointMake(1.0, 0.0) to CGPointMake(0.0, 1.0)
                    Direction.TL_BR -> CGPointMake(0.0, 0.0) to CGPointMake(1.0, 1.0)
                }

                startPoint = start
                endPoint = end
            }
        }
        null -> {
            backgroundLayer = CALayer()
        }
    }

    val border = border
    if (border != null) {
        backgroundLayer.borderWidth = border.width.toDouble()
        backgroundLayer.borderColor = border.color.toUIColor().CGColor
    }

    when (val shape = shape) {
        is Shape.Rectangle -> {
            val cornerRadius = shape.cornerRadius
            if (cornerRadius != null) {
                backgroundLayer.masksToBounds = true

                // FIXME memoryleak.
                backgroundLayer.displayLink {
                    CATransaction.begin()
                    CATransaction.setDisableActions(true)

                    val minEdge = backgroundLayer.bounds.useContents { min(size.width, size.height) }
                    backgroundLayer.cornerRadius = min(minEdge / 2, cornerRadius.toDouble())

                    CATransaction.commit()
                }
            }
        }
        is Shape.Oval -> {
            TODO()
        }
    }
    return backgroundLayer
}

fun UIButton.applyStateBackgroundIfNeeded(background: StateBackground?) {
    if (background == null) return

    adjustsImageWhenDisabled = false
    adjustsImageWhenHighlighted = false

    val normalBg = background.normal.caLayer().also {
        layer.addSublayer(it)
    }
    val disabledBg = background.disabled.caLayer().also {
        layer.addSublayer(it)
    }
    val pressedBg = background.pressed.caLayer().also {
        layer.addSublayer(it)
    }

    fun updateLayers() {
        if (!isEnabled()) {
            disabledBg.opacity = 1.0f
            normalBg.opacity = 0f
            pressedBg.opacity = 0f
            return
        }

        if (isHighlighted()) {
            pressedBg.opacity = 1.0f
            normalBg.opacity = 0f
        } else {
            normalBg.opacity = 1.0f
            pressedBg.opacity = 0f
        }
        disabledBg.opacity = 0f
    }

    updateLayers()

    // FIXME memoryleak, perfomance problem !!!
    displayLink {
        val (width, height) = layer.bounds.useContents { size.width to size.height }

        CATransaction.begin()
        CATransaction.setDisableActions(true)

        normalBg.frame = CGRectMake(0.0, 0.0, width, height)
        disabledBg.frame = CGRectMake(0.0, 0.0, width, height)
        pressedBg.frame = CGRectMake(0.0, 0.0, width, height)

        updateLayers()

        CATransaction.commit()
    }
}

fun UIView.applyBackgroundIfNeeded(background: Background?) {
    if (background == null) return

    this.backgroundColor = UIColor.clearColor

    val bgLayer = background.caLayer()
    layer.insertSublayer(bgLayer, 0)

    // FIXME memoryleak, perfomance problem !!!
    displayLink {
        val (width, height) = layer.bounds.useContents { size.width to size.height }

        CATransaction.begin()
        CATransaction.setDisableActions(true)

        bgLayer.frame = CGRectMake(0.0, 0.0, width, height)

        CATransaction.commit()
    }
}

fun UIControl.applyStateBackgroundIfNeeded(stateBackground: StateBackground?) {
    if (stateBackground == null) return

    // TODO complete it
    applyBackgroundIfNeeded(stateBackground.normal)
}
 