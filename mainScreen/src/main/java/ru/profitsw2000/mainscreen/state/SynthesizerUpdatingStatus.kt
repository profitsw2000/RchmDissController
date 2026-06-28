package ru.profitsw2000.mainscreen.state

import ru.profitsw2000.data.model.bluetooth.state.rcd.SynthesizerModuleStateModel

sealed class SynthesizerUpdatingStatus {
    data class Idle(val synthesizerModuleStateModel: SynthesizerModuleStateModel): SynthesizerUpdatingStatus()
    data object Updating: SynthesizerUpdatingStatus()
    data object Success: SynthesizerUpdatingStatus()
    data class Error(val errorCode: Int): SynthesizerUpdatingStatus()
}