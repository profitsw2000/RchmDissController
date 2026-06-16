package ru.profitsw2000.data.model.bluetooth.state.rcd

data class OutputModuleState(
    val transmitterIsOn: Boolean = false,
    val lfmExtTriggerIsOn: Boolean = false,
    val transmitterDetectorVoltage: Int = -1,
    val secondaryPowerSourceVoltage: Int = -1
)
