package com.griffinrobotics.lib.control

import com.griffinrobotics.lib.util.NanoClock
import com.griffinrobotics.lib.util.epsilonEquals
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

/**
 * PID controller with various feedforward components. The [feedforward] coefficients are designed for DC motor feedforward
 * control (the most common kind of feedforward in FTC). [kF] provides a custom feedforward term for other plants.
 *
 * @param pid traditional PID coefficients
 * @param feedforward motor feedforward coefficients
 * @param kF custom feedforward that depends on position and/or velocity (e.g., a gravity term for arms)
 * @param clock clock
 */
class PIDFController
@JvmOverloads constructor(
    var pid: PIDCoefficients,
    var feedforward: FeedforwardCoefficients = FeedforwardCoefficients(),
    private val kF: (Double, Double?) -> Double = { _, _ -> 0.0 },
    private val clock: NanoClock = NanoClock.system()
) {
    private var errorSum: Double = 0.0
    private var lastUpdateTimestamp: Double = Double.NaN

    private var inputBounded: Boolean = false
    private var minInput: Double = 0.0
    private var maxInput: Double = 0.0

    private var outputBounded: Boolean = false
    private var minOutput: Double = 0.0
    private var maxOutput: Double = 0.0

    /**
     * Target position (that is, the controller setpoint).
     */
    var targetPosition: Double = 0.0

    /**
     * Target velocity.
     */
    var targetVelocity: Double = 0.0

    /**
     * Target acceleration.
     */
    var targetAcceleration: Double = 0.0

    /**
     * Error computed in the last call to [update].
     */
    var lastError: Double = 0.0
        private set

    /**
     * The position error considered tolerable for [isAtSetPoint] to return true.
     */
    var tolerance: Double = 0.05

    /**
     * Returns whether the controller is at the target position.
     */
    fun isAtSetPoint() = abs(lastError) <= tolerance

    /**
     * Sets bound on the input of the controller. The min and max values are considered modularly-equivalent (that is,
     * the input wraps around).
     *
     * @param min minimum input
     * @param max maximum input
     */
    fun setInputBounds(min: Double, max: Double) {
        if (min < max) {
            inputBounded = true
            minInput = min
            maxInput = max
        }
    }

    /**
     * Sets bounds on the output of the controller.
     *
     * @param min minimum output
     * @param max maximum output
     */
    fun setOutputBounds(min: Double, max: Double) {
        if (min < max) {
            outputBounded = true
            minOutput = min
            maxOutput = max
        }
    }

    private fun getPositionError(measuredPosition: Double): Double {
        var error = targetPosition - measuredPosition
        if (inputBounded) {
            val inputRange = maxInput - minInput
            while (abs(error) > inputRange / 2.0) {
                error -= sign(error) * inputRange
            }
        }
        return error
    }

    /**
     * Run a single iteration of the controller.
     *
     * @param measuredPosition measured position (feedback)
     * @param measuredVelocity measured velocity
     */
    @JvmOverloads
    fun update(
        measuredPosition: Double,
        measuredVelocity: Double? = null
    ): Double {
        val currentTimestamp = clock.seconds()
        val error = getPositionError(measuredPosition)
        return if (lastUpdateTimestamp.isNaN()) {
            lastError = error
            lastUpdateTimestamp = currentTimestamp
            0.0
        } else {
            val dt = currentTimestamp - lastUpdateTimestamp
            errorSum += 0.5 * (error + lastError) * dt
            val errorDeriv =
                (measuredVelocity?.let { targetVelocity - it } ?: (error - lastError) / dt)
            val filteredDeriv = (errorDeriv * pid.N) / (errorDeriv + pid.N)

            lastError = error
            lastUpdateTimestamp = currentTimestamp

            // note: we'd like to refactor this with Kinematics.calculateMotorFeedforward() but kF complicates the
            // determination of the sign of kStatic
            val baseOutput = pid.kP * error + pid.kI * errorSum +
                    pid.kD * filteredDeriv +
                    feedforward.kV * targetVelocity + feedforward.kA * targetAcceleration + kF(
                measuredPosition,
                measuredVelocity
            )
            val output =
                if (baseOutput epsilonEquals 0.0) 0.0 else baseOutput + sign(baseOutput) * feedforward.kStatic

            if (outputBounded) {
                if (output != max(
                        minOutput,
                        min(output, maxOutput)
                    ) && sign(error) == sign(output)
                ) {
                    errorSum -= 0.5 * (error + lastError) * dt
                }
                max(minOutput, min(output, maxOutput))
            } else {
                output
            }
        }
    }

    /**
     * Reset the controller's integral sum.
     */
    fun reset() {
        errorSum = 0.0
        lastError = 0.0
        lastUpdateTimestamp = Double.NaN
    }
}