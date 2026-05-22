package ru.profitsw2000.data.data.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.profitsw2000.core.drawable.utils.CTR1_REGISTER_COMMAND
import ru.profitsw2000.core.drawable.utils.CTR2_REGISTER_COMMAND
import ru.profitsw2000.core.drawable.utils.CTR3_REGISTER_COMMAND
import ru.profitsw2000.core.drawable.utils.FRAC_REGISTER_COMMAND
import ru.profitsw2000.core.drawable.utils.INT_REGISTER_COMMAND
import ru.profitsw2000.core.drawable.utils.LFM1_REGISTER_COMMAND
import ru.profitsw2000.core.drawable.utils.LFM2_REGISTER_COMMAND
import ru.profitsw2000.core.drawable.utils.LFM3_REGISTER_COMMAND
import ru.profitsw2000.core.drawable.utils.MOD_REGISTER_COMMAND
import ru.profitsw2000.core.drawable.utils.PRA_REGISTER_COMMAND
import ru.profitsw2000.core.drawable.utils.PRW_REGISTER_COMMAND
import ru.profitsw2000.core.drawable.utils.REF_REGISTER_COMMAND
import ru.profitsw2000.core.drawable.utils.SYNTHESIZER_REGISTERS_TYPE_MASK
import ru.profitsw2000.data.domain.state.RchmDissStateRepository
import ru.profitsw2000.data.mapper.PacketBytesConverter
import ru.profitsw2000.data.model.bluetooth.state.rcd.RchmDissState
import ru.profitsw2000.data.model.bluetooth.state.rcd.SynthesizerModuleState
import ru.profitsw2000.data.model.rcd.RcdInputPacketType

class RchmDissStateRepositoryImpl() : RchmDissStateRepository {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _rchmDissState: MutableStateFlow<RchmDissState> = MutableStateFlow(RchmDissState())
    override val rchmDissState: StateFlow<RchmDissState>
        get() = _rchmDissState
    private val _lastPacket: MutableSharedFlow<RcdInputPacketType> = MutableSharedFlow(replay = 0)
    override val lastPacket: SharedFlow<RcdInputPacketType>
        get() = _lastPacket
    private val packetBytesConverter = PacketBytesConverter()

    override fun updateTransmitterModuleState(byte: Byte) {
        repositoryScope.launch {
            _lastPacket.emit(RcdInputPacketType.TransmitterStateInputPacket)
        }
        _rchmDissState.value = rchmDissState.value.copy(
            transmitterModuleState = packetBytesConverter.transmitterByte(byte)
        )
    }

    override fun updateReceiverModuleState(lowByte: Byte, highByte: Byte) {
        repositoryScope.launch {
            _lastPacket.emit(RcdInputPacketType.ReceiverStateInputPacket)
        }
        _rchmDissState.value = rchmDissState.value.copy(
            receiverModuleState = packetBytesConverter.receiverBytes(lowByte, highByte)
        )
    }

    override fun updateSynthesizerModuleState(lowByte: Byte, middleByte: Byte, highByte: Byte) {
        repositoryScope.launch {
            _lastPacket.emit(RcdInputPacketType.SynthesizerStateInputPacket)
        }
        _rchmDissState.value = rchmDissState.value.copy(
            synthesizerModuleState = getNewSynthesizerState(
                packetBytesConverter.synthesizerBytes(lowByte, middleByte, highByte)
            )
        )
    }

    private fun getNewSynthesizerState(register: Int): SynthesizerModuleState {
        return when(register and SYNTHESIZER_REGISTERS_TYPE_MASK) {
            REF_REGISTER_COMMAND -> rchmDissState.value.synthesizerModuleState.copy(refRegister = register)
            INT_REGISTER_COMMAND -> rchmDissState.value.synthesizerModuleState.copy(intRegister = register)
            FRAC_REGISTER_COMMAND -> rchmDissState.value.synthesizerModuleState.copy(fracRegister = register)
            MOD_REGISTER_COMMAND -> rchmDissState.value.synthesizerModuleState.copy(modRegister = register)
            CTR1_REGISTER_COMMAND -> rchmDissState.value.synthesizerModuleState.copy(ctr1Register = register)
            CTR2_REGISTER_COMMAND -> rchmDissState.value.synthesizerModuleState.copy(ctr2Register = register)
            CTR3_REGISTER_COMMAND -> rchmDissState.value.synthesizerModuleState.copy(ctr3Register = register)
            LFM1_REGISTER_COMMAND -> rchmDissState.value.synthesizerModuleState.copy(lfm1Register = register)
            LFM2_REGISTER_COMMAND -> rchmDissState.value.synthesizerModuleState.copy(lfm2Register = register)
            LFM3_REGISTER_COMMAND -> rchmDissState.value.synthesizerModuleState.copy(lfm3Register = register)
            PRW_REGISTER_COMMAND -> rchmDissState.value.synthesizerModuleState.copy(prwRegister = register)
            PRA_REGISTER_COMMAND -> rchmDissState.value.synthesizerModuleState.copy(praRegister = register)
            else -> rchmDissState.value.synthesizerModuleState
        }
    }

    private fun getSynthesizerInputPacketType(register: Int): RcdInputPacketType {
        return when(register and SYNTHESIZER_REGISTERS_TYPE_MASK) {
            REF_REGISTER_COMMAND -> RcdInputPacketType.SynthesizerRefRegisterInputPacket
            INT_REGISTER_COMMAND -> RcdInputPacketType.SynthesizerIntRegisterInputPacket
            FRAC_REGISTER_COMMAND -> RcdInputPacketType.SynthesizerFracRegisterInputPacket
            MOD_REGISTER_COMMAND -> RcdInputPacketType.SynthesizerModRegisterInputPacket
            CTR1_REGISTER_COMMAND -> RcdInputPacketType.SynthesizerCtr1RegisterInputPacket
            CTR2_REGISTER_COMMAND -> RcdInputPacketType.SynthesizerCtr2RegisterInputPacket
            CTR3_REGISTER_COMMAND -> RcdInputPacketType.SynthesizerCtr3RegisterInputPacket
            LFM1_REGISTER_COMMAND -> RcdInputPacketType.SynthesizerLfm1RegisterInputPacket
            LFM2_REGISTER_COMMAND -> RcdInputPacketType.SynthesizerLfm2RegisterInputPacket
            LFM3_REGISTER_COMMAND -> RcdInputPacketType.SynthesizerLfm3RegisterInputPacket
            PRW_REGISTER_COMMAND -> RcdInputPacketType.SynthesizerPrwRegisterInputPacket
            PRA_REGISTER_COMMAND -> RcdInputPacketType.SynthesizerPraRegisterInputPacket
            else -> RcdInputPacketType.InvalidInputPacket
        }
    }
}