package ru.profitsw2000.data.model.bluetooth.state.rcd

data class OutputModuleState(
    val rchmDissDigitalOutput: UShort = 0x2u,
    val transmitterDetectorVoltage: UShort = 0u,
    val secondaryPowerSourceVoltage: UShort = 0u
)
