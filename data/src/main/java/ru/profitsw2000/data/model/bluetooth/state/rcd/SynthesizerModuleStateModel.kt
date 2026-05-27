package ru.profitsw2000.data.model.bluetooth.state.rcd

data class SynthesizerModuleStateModel(
    val radiationMode: RadiationMode = RadiationMode.NONE,
    val cwFrequency: Long = 13_325_000_000,
    val lowestLfmFrequency: Long = 13_250_000_000,
    val highestLfmFrequency: Long = 13_400_000_000,
    val lfmPeriod: Double = 0.025,
    val isSymmetricLfm: Boolean = false
)

enum class RadiationMode {
    NONE,
    CW,
    LFM
}
