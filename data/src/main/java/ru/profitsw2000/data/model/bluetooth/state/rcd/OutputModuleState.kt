package ru.profitsw2000.data.model.bluetooth.state.rcd

data class OutputModuleState(
    val rchmDissOutput: Byte = 0x2,
    val transmitterDetectorVoltage: UShort = 0u,
    val secondaryPowerSourceVoltage: UShort = 0u
)
