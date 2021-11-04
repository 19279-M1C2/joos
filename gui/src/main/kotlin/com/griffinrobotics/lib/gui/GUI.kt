package com.griffinrobotics.lib.gui

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.griffinrobotics.lib.gui.style.Theme
import com.griffinrobotics.lib.gui.trajectory.WaypointBuilder
import com.griffinrobotics.lib.gui.trajectory.Waypoints
import com.griffinrobotics.lib.trajectory.config.TrajectoryConstraints
import tornadofx.launch

/**
 * Class for interacting with the application.
 */
class GUI {
    private val args = HashMap<String, String>()
    private val mapper = JsonMapper()

    init {
        mapper.registerKotlinModule()
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    /**
     * Sets the trajectory to be opened on startup.
     */
    fun followTrajectory(trajectory: Waypoints): GUI {
        val string = mapper.writeValueAsString(trajectory)
        args["trajectory"] = string
        return this
    }

    /**
     * Sets the constraints to be obeyed on startup.
     */
    fun setConstraints(constraints: TrajectoryConstraints): GUI {
        val string = mapper.writeValueAsString(constraints)
        args["constraints"] = string
        return this
    }

    fun setTheme(theme: Theme): GUI {
        val string = mapper.writeValueAsString(theme)
        args["theme"] = string
        return this
    }

    /**
     * Uses the provided arguments and launches the application.
     *
     *
     * *Note*: This method must only be called *once*.
     */
    fun start() {
        launch<MainApp>(args.map { "--${it.key}=${it.value}" }.toTypedArray())
    }
}