package ru.profitsw2000.data.model.bluetooth.state.rcd

data class SynthesizerModuleState(
    val refRegister: Int = 1,
    val intRegister: Int = 12,
    val fracRegister: Int = 0,
    val modRegister: Int = 0,
    val ctr1Register: Int = 0x600,
    val ctr2Register: Int = 0x3F,
    val ctr3Register: Int = 0x15,
    val lfm1Register: Int = 0,
    val lfm2Register: Int = 0,
    val lfm3Register: Int = 0,
    val prwRegister: Int = 0,
    val praRegister: Int = 0,
    val radiationMode: RadiationMode = RadiationMode.NONE,
    val cwFrequency: Long = 13320000000,
    val lfmStartFrequency: Long = 13250000000,
    val lfmStopFrequency: Long = 13400000000,
    val lfmPeriod: Float = 50f
){
    val someField: Int = refRegister + intRegister
}

enum class RadiationMode {
    NONE,
    CW,
    LFM
}
