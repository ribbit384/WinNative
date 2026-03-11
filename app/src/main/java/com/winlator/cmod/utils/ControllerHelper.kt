package com.winlator.cmod.utils

import android.view.InputDevice

object ControllerHelper {
    fun isControllerConnected(): Boolean {
        val deviceIds = InputDevice.getDeviceIds()
        for (deviceId in deviceIds) {
            val device = InputDevice.getDevice(deviceId) ?: continue
            val sources = device.sources
            if ((sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK) {
                return true
            }
        }
        return false
    }

    fun isPlayStationController(): Boolean {
        val deviceIds = InputDevice.getDeviceIds()
        for (deviceId in deviceIds) {
            val device = InputDevice.getDevice(deviceId) ?: continue
            val name = device.name.lowercase()
            if (name.contains("dualshock") || name.contains("playstation") || name.contains("dualsense") || name.contains("wireless controller")) {
                return true
            }
        }
        return false
    }
}
