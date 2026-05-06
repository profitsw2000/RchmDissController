package ru.profitsw2000.data.model.bluetooth.state.rcd

data class ReceiverModuleState(
    val enabledChannelNumber: Int = 0,
    val testSignalIsEnabled: Boolean = false,
    val lockedInputChannels: BooleanArray = BooleanArray(5) { true },
    val inputAttenuationValue: Int = 62
)
