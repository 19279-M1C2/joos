package com.griffinrobotics.lib.control

import com.griffinrobotics.lib.kinematics.Kinematics

/**
 * Feedforward gains used by [Kinematics.calculateMotorFeedforward].
 * @param kV velocity gain
 * @param kA acceleration gain
 * @param kStatic additive constant
 */
data class FeedforwardCoefficients(
    @JvmField var kV: Double = 0.0,
    @JvmField var kA: Double = 0.0,
    @JvmField var kStatic: Double = 0.0
)