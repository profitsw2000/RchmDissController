package ru.profitsw2000.data.domain.state

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import ru.profitsw2000.data.model.bluetooth.state.rcd.RchmDissState
import ru.profitsw2000.data.model.rcd.RcdInputPacketType

interface RchmDissStateRepository {
    val rchmDissState: StateFlow<RchmDissState>
    val lastPacket: SharedFlow<RcdInputPacketType>

    fun updateTransmitterModuleState(byte: Byte)

    fun updateReceiverModuleState(lowByte: Byte, highByte: Byte)

    fun updateSynthesizerModuleState(byteArray: ByteArray)
}