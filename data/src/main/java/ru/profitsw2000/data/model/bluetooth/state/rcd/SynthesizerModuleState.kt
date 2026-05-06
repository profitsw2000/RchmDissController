package ru.profitsw2000.data.model.bluetooth.state.rcd

data class SynthesizerModuleState(
    val radiationMode: RadiationMode = RadiationMode.NONE,
    val cwFrequency: Long = 13320000000,
    val lfmStartFrequency: Long = 13250000000,
    val lfmStopFrequency: Long = 13400000000,
    val lfmPeriod: Float = 50f
)

enum class RadiationMode {
    NONE,
    CW,
    LFM
}
