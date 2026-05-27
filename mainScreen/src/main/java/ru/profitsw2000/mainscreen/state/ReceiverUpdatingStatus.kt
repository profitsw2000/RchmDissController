package ru.profitsw2000.mainscreen.state

import ru.profitsw2000.data.model.bluetooth.state.rcd.ReceiverModuleState

sealed class ReceiverUpdatingStatus {
    data object Idle: ReceiverUpdatingStatus()
    data object Updating: ReceiverUpdatingStatus()
    data class Success(val receiverModuleState: ReceiverModuleState): ReceiverUpdatingStatus()
    data class Error(val errorCode: Int): ReceiverUpdatingStatus()
}