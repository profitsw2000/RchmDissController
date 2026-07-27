package ru.profitsw2000.data.model.bluetooth.state.rcd

import ru.profitsw2000.core.drawable.utils.ATTENUATOR_BIT_MASK

data class ReceiverModuleState(
    val enabledChannelNumber: Int = 0,
    val testSignalIsEnabled: Boolean = false,
    val lockedInputChannels: BooleanArray = BooleanArray(5) { true },
    val inputAttenuationValue: Int = 62,
    val inputAttenuatorsCode: Int = ATTENUATOR_BIT_MASK
)
