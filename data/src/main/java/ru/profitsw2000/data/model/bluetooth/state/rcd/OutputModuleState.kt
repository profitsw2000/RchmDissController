package ru.profitsw2000.data.model.bluetooth.state.rcd

data class OutputModuleState(
    val lfmExtTriggerIsOn: Boolean = false,
    val transmitterIsOn: Boolean = true,
    val pllIsLocked: Boolean = false,
    val transmitterDetectorVoltage: Double = 0.0,
    val secondaryPowerSourceVoltage: Double = 0.0
)
