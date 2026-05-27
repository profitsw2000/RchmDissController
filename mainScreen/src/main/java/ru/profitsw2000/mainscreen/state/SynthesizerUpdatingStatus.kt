package ru.profitsw2000.mainscreen.state

import ru.profitsw2000.data.model.bluetooth.state.rcd.ReceiverModuleState
import ru.profitsw2000.data.model.bluetooth.state.rcd.SynthesizerModuleStateModel

sealed class SynthesizerUpdatingStatus {
    data object Idle: SynthesizerUpdatingStatus()
    data object Updating: SynthesizerUpdatingStatus()
    data class Success(val synthesizerModuleStateModel: SynthesizerModuleStateModel): SynthesizerUpdatingStatus()
    data class Error(val errorCode: Int): SynthesizerUpdatingStatus()
}