package ru.profitsw2000.data.model.bluetooth.state.rcd

data class RchmDissStateModel(
    val receiverModuleState: ReceiverModuleState = ReceiverModuleState(),
    val isActualReceiverData: Boolean = false,
    val transmitterModuleState: TransmitterModuleState = TransmitterModuleState(),
    val isActualTransmitterData: Boolean = false,
    val synthesizerModuleState: SynthesizerModuleStateModel = SynthesizerModuleStateModel(),
    val isActualSynthesizerData: Boolean = false,
    val innerModuleTemperature: Double = 250.0,
    val readMemoryValue: Byte = 0xFF.toByte()
)
