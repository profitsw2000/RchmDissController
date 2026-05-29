package ru.profitsw2000.data.model.bluetooth.state.rcd

data class RchmDissState(
    val receiverModuleState: ReceiverModuleState = ReceiverModuleState(),
    val transmitterModuleState: TransmitterModuleState = TransmitterModuleState(),
    val synthesizerModuleState: SynthesizerModuleState = SynthesizerModuleState(),
)
