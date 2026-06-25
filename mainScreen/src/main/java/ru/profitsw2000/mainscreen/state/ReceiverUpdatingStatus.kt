package ru.profitsw2000.mainscreen.state

import ru.profitsw2000.data.model.bluetooth.state.rcd.ReceiverModuleState

sealed class ReceiverUpdatingStatus {
    data class Idle(val receiverModuleState: ReceiverModuleState): ReceiverUpdatingStatus()
    data object Updating: ReceiverUpdatingStatus()
    data object Success: ReceiverUpdatingStatus()
    data class Error(val errorCode: Int): ReceiverUpdatingStatus()
}