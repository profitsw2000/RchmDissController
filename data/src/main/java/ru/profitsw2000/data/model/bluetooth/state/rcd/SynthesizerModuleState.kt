package ru.profitsw2000.data.model.bluetooth.state.rcd

data class SynthesizerModuleState(
    val refRegister: List<Int> = listOf(0x1, 0x1),
    val intRegister: List<Int> = listOf(0x20000C, 0x20000C),
    val fracRegister: List<Int> = listOf(0x400000, 0x400000),
    val modRegister: List<Int> = listOf(0x600000, 0x600000),
    val ctr1Register: List<Int> = listOf(0x800000, 0x800000),
    val ctr2Register: List<Int> = listOf(0xA0003F, 0xA0003F),
    val ctr3Register: List<Int> = listOf(0xC00015, 0xC00015),
    val lfm1Register: List<Int> = listOf(0x100000, 0x100000),
    val lfm2Register: List<Int> = listOf(0x300000, 0x300000),
    val lfm3Register: List<Int> = listOf(0x500000, 0x500000),
    val prwRegister: Int = 0x700000,
    val praRegister: Int = 0x900000
)

fun SynthesizerModuleState.updateRegister(
    newValue: Int,
    selector: (SynthesizerModuleState) -> List<Int>,
    updater: SynthesizerModuleState.(List<Int>) -> SynthesizerModuleState
): SynthesizerModuleState {
    val index = if ((this.prwRegister and 0xFFFFF) == 0) 0
    else 1

    val newList = selector(this).toMutableList().apply { this[index] = newValue }
    return updater(this, newList)
}
