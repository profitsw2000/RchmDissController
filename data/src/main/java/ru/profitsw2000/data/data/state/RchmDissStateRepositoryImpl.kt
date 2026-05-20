package ru.profitsw2000.data.data.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.profitsw2000.data.domain.state.RchmDissStateRepository
import ru.profitsw2000.data.model.bluetooth.state.rcd.RchmDissState
import ru.profitsw2000.data.model.rcd.RcdInputPacketType

class RchmDissStateRepositoryImpl() : RchmDissStateRepository {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _rchmDissState: MutableStateFlow<RchmDissState> = MutableStateFlow(RchmDissState())
    override val rchmDissState: StateFlow<RchmDissState>
        get() = _rchmDissState
    private val _lastPacket: MutableSharedFlow<RcdInputPacketType> = MutableStateFlow(RcdInputPacketType.InvalidInputPacket)
    override val lastPacket: SharedFlow<RcdInputPacketType>
        get() = _lastPacket

    override fun updateTransmitterModuleState(byte: Byte) {
        repositoryScope.launch {
            _lastPacket.emit(RcdInputPacketType.TransmitterStateInputPacket)
        }
    }

    override fun updateReceiverModuleState(lowByte: Byte, highByte: Byte) {
        repositoryScope.launch {
            _lastPacket.emit(RcdInputPacketType.ReceiverStateInputPacket)
        }
    }

    override fun updateSynthesizerModuleState(lowByte: Byte, middleByte: Byte, highByte: Byte) {
        repositoryScope.launch {
            _lastPacket.emit(RcdInputPacketType.SynthesizerStateInputPacket)
        }
    }
}