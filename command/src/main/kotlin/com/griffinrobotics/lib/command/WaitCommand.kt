package com.griffinrobotics.lib.command

import com.griffinrobotics.lib.util.NanoClock

/**
 * A command that waits the specified duration before finishing.
 *
 * @param duration the duration in seconds
 */
class WaitCommand(var duration: Double) : Command() {
    private val clock = NanoClock.system()
    private var start = clock.seconds()

    override fun init() {
        start = clock.seconds()
    }

    override fun isFinished() = clock.seconds() - start >= duration
}