package ru.profitsw2000.data.model.bluetooth.state.rcd

data class RchmDissStateModel(
    val receiverModuleState: ReceiverModuleState = ReceiverModuleState(),
    val transmitterModuleState: TransmitterModuleState = TransmitterModuleState(),
    val synthesizerModuleState: SynthesizerModuleStateModel = SynthesizerModuleStateModel(),
    val innerModuleTemperature: Double = 250.0,
    val readMemoryValue: Byte = 0xFF.toByte()
)
