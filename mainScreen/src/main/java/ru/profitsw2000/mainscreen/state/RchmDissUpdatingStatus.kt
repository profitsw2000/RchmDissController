package ru.profitsw2000.mainscreen.state

import ru.profitsw2000.data.model.bluetooth.state.rcd.RchmDissState

sealed class RchmDissUpdatingStatus {
    object Idle : RchmDissUpdatingStatus()
    object Updating : RchmDissUpdatingStatus()
    data class Success(val rchmDissState: RchmDissState) : RchmDissUpdatingStatus()
    data class Error(val message: String, val errorCode: Int) : RchmDissUpdatingStatus()
}