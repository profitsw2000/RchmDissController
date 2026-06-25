package ru.profitsw2000.data.data.state

import kotlinx.coroutines.CoroutineDispatcher
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
import ru.profitsw2000.data.model.bluetooth.state.rcd.updateRegister
import ru.profitsw2000.data.model.rcd.RcdInputPacketType

class RchmDissStateRepositoryImpl(
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : RchmDissStateRepository {
    private val repositoryScope = CoroutineScope(SupervisorJob() + defaultDispatcher)
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

    override fun updateOutputModuleState(byteArray: ByteArray) {
        repositoryScope.launch {
            _lastPacket.emit(RcdInputPacketType.RcdOutputControlInputPacket)
        }
        _rchmDissState.value = rchmDissState.value.copy(
            outputModuleState = packetBytesConverter.rcdOutputBytes(byteArray)
        )
    }

    override fun writeModuleTemperature(lowByte: Byte, highByte: Byte) {
        repositoryScope.launch {
            _lastPacket.emit(RcdInputPacketType.RcdTemperatureInputPacket)
        }
        _rchmDissState.value = rchmDissState.value.copy(
            innerModuleTemperature = packetBytesConverter.rcdTemperatureBytes(lowByte, highByte)
        )
    }

    override fun writeModuleMemoryByte(byte: Byte) {
        repositoryScope.launch {
            _lastPacket.emit(RcdInputPacketType.RcdMemoryReadInputPacket)
        }
        _rchmDissState.value = rchmDissState.value.copy(
            readMemoryValue = byte
        )
    }

    private fun getNewSynthesizerState(register: Int): SynthesizerModuleState {
        return when(register and SYNTHESIZER_REGISTERS_TYPE_MASK) {
            REF_REGISTER_COMMAND -> rchmDissState.value.synthesizerModuleState.updateRegister(register, {it.refRegister}) {copy(refRegister = it)}
            INT_REGISTER_COMMAND -> rchmDissState.value.synthesizerModuleState.updateRegister(register, {it.intRegister}) {copy(intRegister = it)}
            FRAC_REGISTER_COMMAND -> rchmDissState.value.synthesizerModuleState.updateRegister(register, {it.fracRegister}) {copy(fracRegister = it)}
            MOD_REGISTER_COMMAND -> rchmDissState.value.synthesizerModuleState.updateRegister(register, {it.modRegister}) {copy(modRegister = it)}
            CTR1_REGISTER_COMMAND -> rchmDissState.value.synthesizerModuleState.updateRegister(register, {it.ctr1Register}) {copy(ctr1Register = it)}
            CTR2_REGISTER_COMMAND -> rchmDissState.value.synthesizerModuleState.updateRegister(register, {it.ctr2Register}) {copy(ctr2Register = it)}
            CTR3_REGISTER_COMMAND -> rchmDissState.value.synthesizerModuleState.updateRegister(register, {it.ctr3Register}) {copy(ctr3Register = it)}
            LFM1_REGISTER_COMMAND -> rchmDissState.value.synthesizerModuleState.updateRegister(register, {it.lfm1Register}) {copy(lfm1Register = it)}
            LFM2_REGISTER_COMMAND -> rchmDissState.value.synthesizerModuleState.updateRegister(register, {it.lfm2Register}) {copy(lfm2Register = it)}
            LFM3_REGISTER_COMMAND -> rchmDissState.value.synthesizerModuleState.updateRegister(register, {it.lfm3Register}) {copy(lfm3Register = it)}
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