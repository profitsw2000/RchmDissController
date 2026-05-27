package ru.profitsw2000.mainscreen.state

import ru.profitsw2000.data.model.bluetooth.state.rcd.TransmitterModuleState

sealed class TransmitterUpdatingStatus {
    data object Idle: TransmitterUpdatingStatus()
    data object Updating: TransmitterUpdatingStatus()
    data class Success(val transmitterModuleState: TransmitterModuleState): TransmitterUpdatingStatus()
    data class Error(val errorCode: Int): TransmitterUpdatingStatus()
}