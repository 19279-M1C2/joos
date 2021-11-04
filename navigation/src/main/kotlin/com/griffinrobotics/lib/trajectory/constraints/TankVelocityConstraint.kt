package com.griffinrobotics.lib.trajectory.constraints

import com.griffinrobotics.lib.geometry.Pose2d
import com.griffinrobotics.lib.kinematics.Kinematics
import com.griffinrobotics.lib.kinematics.TankKinematics
import kotlin.math.abs
import kotlin.math.max

/**
 * Tank-specific drive constraints that also limit maximum wheel velocities.
 *
 * @param maxWheelVel maximum wheel velocity
 * @param trackWidth track width
 */
open class TankVelocityConstraint(
    val maxWheelVel: Double,
    val trackWidth: Double
) : TrajectoryVelocityConstraint {
    override fun get(s: Double, pose: Pose2d, deriv: Pose2d, baseRobotVel: Pose2d): Double {
        val wheel0 = TankKinematics.robotToWheelVelocities(baseRobotVel, trackWidth)
        if (wheel0.map(::abs).maxOrNull()!! >= maxWheelVel) {
            throw UnsatisfiableConstraint()
        }

        val robotDeriv = Kinematics.fieldToRobotVelocity(pose, deriv)

        val wheel = TankKinematics.robotToWheelVelocities(robotDeriv, trackWidth)
        return wheel0.zip(wheel).map {
            max(
                (maxWheelVel - it.first) / it.second,
                (-maxWheelVel - it.first) / it.second
            )
        }.minOrNull()!!
    }
}
