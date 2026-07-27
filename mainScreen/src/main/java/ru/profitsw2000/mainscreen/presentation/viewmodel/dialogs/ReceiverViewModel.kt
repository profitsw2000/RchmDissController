package ru.profitsw2000.mainscreen.presentation.viewmodel.dialogs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import ru.profitsw2000.core.drawable.utils.RESPONSE_PACKET_TIMEOUT_ERROR_CODE
import ru.profitsw2000.core.drawable.utils.UNKNOWN_ERROR_CODE
import ru.profitsw2000.data.domain.bluetooth.BluetoothPacketManager
import ru.profitsw2000.data.domain.bluetooth.BluetoothRepository
import ru.profitsw2000.data.domain.state.RchmDissStateRepository
import ru.profitsw2000.data.model.rcd.RcdInputPacketType
import ru.profitsw2000.mainscreen.state.ReceiverUpdatingStatus
import kotlin.time.Duration.Companion.milliseconds

class ReceiverViewModel(
    private val rchmDissStateRepository: RchmDissStateRepository,
    private val bluetoothRepository: BluetoothRepository,
    private val bluetoothPacketManager: BluetoothPacketManager
) : ViewModel() {
    private val _receiverUpdatingStatusFlow = MutableStateFlow<ReceiverUpdatingStatus>(
        ReceiverUpdatingStatus.Idle(rchmDissStateRepository.rchmDissState.value.receiverModuleState)
    )
    val receiverUpdatingStatusFlow: StateFlow<ReceiverUpdatingStatus> = _receiverUpdatingStatusFlow


    fun updateReceiver(byteArray: ByteArray) {
        viewModelScope.launch {
            _receiverUpdatingStatusFlow.value = ReceiverUpdatingStatus.Updating

            try {
                bluetoothRepository.bluetoothDataRepository.writeData(
                    bluetoothPacketManager.getWriteToReceiverPacket(byteArray)
                )
                withTimeout(5000L.milliseconds) {
                    rchmDissStateRepository.lastPacket.first {
                        it == RcdInputPacketType.ReceiverStateInputPacket
                    }
                }

                _receiverUpdatingStatusFlow.value =
                    ReceiverUpdatingStatus.Success

                delay(500.milliseconds)

                _receiverUpdatingStatusFlow.value =
                    ReceiverUpdatingStatus.Idle(
                        rchmDissStateRepository.rchmDissState.value.receiverModuleState
                    )

            } catch (exc: TimeoutCancellationException) {
                _receiverUpdatingStatusFlow.value = ReceiverUpdatingStatus.Error(RESPONSE_PACKET_TIMEOUT_ERROR_CODE)
            } catch (exc: Exception) {
                _receiverUpdatingStatusFlow.value = ReceiverUpdatingStatus.Error(UNKNOWN_ERROR_CODE)
            }
        }
    }

}