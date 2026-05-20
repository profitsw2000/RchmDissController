package ru.profitsw2000.data.model.bluetooth.state.rcd

data class SynthesizerModuleState(
    val refRegister: Int = 0x1,
    val intRegister: Int = 0x20000C,
    val fracRegister: Int = 0x400000,
    val modRegister: Int = 0x600000,
    val ctr1Register: Int = 0x800000,
    val ctr2Register: Int = 0xA0003F,
    val ctr3Register: Int = 0xC00015,
    val lfm1Register: Int = 0x100000,
    val lfm2Register: Int = 0x300000,
    val lfm3Register: Int = 0x500000,
    val prwRegister: Int = 0x700000,
    val praRegister: Int = 0x900000,
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
