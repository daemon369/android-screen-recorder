package me.daemon.screenrecorder.demo

import me.daemon.infrastructure.application.InfrastructureApp
import me.daemon.logger.api.Level
import me.daemon.logger.defaultLevel
import me.daemon.logger.defaultShowCaller
import me.daemon.logger.defaultTag
import me.daemon.logger.openLog

class App : InfrastructureApp() {

    init {
        openLog = true
        defaultShowCaller = true
        defaultLevel = Level.DEBUG
        defaultTag = "Daemon-Screen-Recorder"
    }

}