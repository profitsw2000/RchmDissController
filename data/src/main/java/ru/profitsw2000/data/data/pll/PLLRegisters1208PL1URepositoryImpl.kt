package ru.profitsw2000.data.data.pll

import ru.profitsw2000.core.drawable.utils.LFM1_REGISTER_COMMAND
import ru.profitsw2000.data.domain.pll.PLLRegisters1208PL1URepository
import ru.profitsw2000.data.model.bluetooth.state.rcd.RadiationMode
import ru.profitsw2000.data.model.bluetooth.state.rcd.SynthesizerModuleState
import ru.profitsw2000.data.model.bluetooth.state.rcd.SynthesizerModuleStateModel
import ru.profitsw2000.data.model.pll.LfmInputParametersModel

const val REF_REG_CW = 0x10
const val FRAC_REG_CW = 0x4001F4
const val MOD_REG_CW = 0x6001F4
const val MOD = 32_000
const val Fref = 20_000_000L
const val CTR1_RST_CW = 0x890689
const val CTR1_CW = 0x890688
const val CTR1_RST = 0x840609
const val CTR1 = 0x840608
const val CTR2_CW = 0xA00005
const val CTR2 = 0xA00002
const val CTR3 = 0xC00001
const val LFM3 = 0x500204
const val LFM31 = 0x500006
const val LFM3_NON_SYM = 0x500004
const val INT_REG = 0x200000
const val FRAC_REG = 0x400000
const val LFM1_REG = 0x100000
const val LFM2_REG = 0x300000
const val LFM3_REG = 0x500000
const val MOD_REG = 0x607D00
const val PRW0_REG = 0x700000
const val PRA0_REG = 0x900000
const val PRW1_REG = 0x704000

class PLLRegisters1208PL1URepositoryImpl : PLLRegisters1208PL1URepository {

    override suspend fun getLfmRegisters(lfmInputParametersModel: LfmInputParametersModel): List<Int> {
        return buildList {
            addAll(getLfmRegistersFirstProfile(lfmInputParametersModel))
            addAll(getLfmRegistersSecondProfile(lfmInputParametersModel))
            add(PRA0_REG)
        }
    }

    override suspend fun getCwRegisters(frequency: Long): List<Int> {
        return listOf(
            PRW0_REG,
            REF_REG_CW,
            getIntRegisterCw(frequency),
            FRAC_REG_CW,
            MOD_REG_CW,
            CTR1_RST_CW,
            CTR1_CW,
            CTR2_CW,
            CTR3,
            LFM1_REG,
            LFM2_REG,
            LFM3_REG,
            PRA0_REG
        )
    }

    override suspend fun getLfmParameters(synthesizerModuleState: SynthesizerModuleState): SynthesizerModuleStateModel {
        val radiationMode = getRadiationMode(synthesizerModuleState)
        //val cwFrequency = if (radiationMode == RadiationMode.CW)
        return TODO()
    }

    private fun getLfmRegistersFirstProfile(lfmInputParametersModel: LfmInputParametersModel): List<Int> = with(lfmInputParametersModel) {
        return listOf(
            PRW0_REG,
            getRefRegister(lfmDeviationPeriod, isSymmetricLfm),
            getIntRegister(lowestLfmFrequency, lfmDeviationPeriod, isSymmetricLfm),
            getFracRegister(lowestLfmFrequency, lfmDeviationPeriod, isSymmetricLfm),
            MOD_REG,
            CTR1_RST,
            CTR1,
            CTR2,
            CTR3,
            getLfm1Register(lowestLfmFrequency, highestLfmFrequency, lfmDeviationPeriod, isSymmetricLfm),
            getLfm3Register(isSymmetricLfm),
            getLfm2Register(lfmDeviationPeriod, isSymmetricLfm)
        )
    }

    private fun getLfmRegistersSecondProfile(lfmInputParametersModel: LfmInputParametersModel): List<Int> = with(lfmInputParametersModel) {
        return if (isSymmetricLfm) listOf(
            PRW1_REG,
            getRefRegister(lfmDeviationPeriod, isSymmetricLfm),
            getInt1Register(lowestLfmFrequency, lfmDeviationPeriod, isSymmetricLfm),
            getFrac1Register(lowestLfmFrequency, lfmDeviationPeriod, isSymmetricLfm),
            MOD_REG,
            CTR1_RST,
            CTR1,
            CTR2,
            CTR3,
            getLfm1Register(lowestLfmFrequency, highestLfmFrequency, lfmDeviationPeriod, isSymmetricLfm),
            LFM31,
            getLfm2Register(lfmDeviationPeriod, isSymmetricLfm)
        ) else listOf()
    }

    private fun getRefRegister(
        lfmDeviationPeriod: Double,
        isSymmetricLfm: Boolean
    ): Int {
        return if (lfmDeviationPeriod <= 0.05 || isSymmetricLfm) 1
        else 2
    }

    private fun getIntRegister(
        lowestLfmFrequency: Long,
        lfmDeviationPeriod: Double,
        isSymmetricLfm: Boolean
    ): Int {
        val ref = getRefRegister(lfmDeviationPeriod, isSymmetricLfm)
        return (((lowestLfmFrequency*ref)/(4* Fref)).toInt()) or INT_REG
    }

    private fun getInt1Register(
        highestLfmFrequency: Long,
        lfmDeviationPeriod: Double,
        isSymmetricLfm: Boolean
    ): Int {
        val ref = getRefRegister(lfmDeviationPeriod, isSymmetricLfm)
        return (((highestLfmFrequency*ref)/(4* Fref)).toInt()) or INT_REG
    }

    private fun getFracRegister(
        lowestLfmFrequency: Long,
        lfmDeviationPeriod: Double,
        isSymmetricLfm: Boolean
    ): Int {
        val ref = getRefRegister(lfmDeviationPeriod, isSymmetricLfm)
        val fractionalMultPart = (lowestLfmFrequency*ref)%(4* Fref)
        return (((MOD *ref*fractionalMultPart)/(4* Fref)).toInt()) or FRAC_REG
    }

    private fun getFrac1Register(
        highestLfmFrequency: Long,
        lfmDeviationPeriod: Double,
        isSymmetricLfm: Boolean
    ): Int {
        val ref = getRefRegister(lfmDeviationPeriod, isSymmetricLfm)
        val fractionalMultPart = (highestLfmFrequency*ref)%(4* Fref)
        return (((MOD *ref*fractionalMultPart)/(4* Fref)).toInt()) or FRAC_REG
    }

    private fun getLfm2Register(
        lfmDeviationPeriod: Double,
        isSymmetricLfm: Boolean
    ): Int {
        val Fpfd = Fref /getRefRegister(lfmDeviationPeriod, isSymmetricLfm)
        var sawStep = 4000
        val fracIncRemain = if(isSymmetricLfm) ((lfmDeviationPeriod*Fpfd)%(2*sawStep)).toInt()
        else ((lfmDeviationPeriod*Fpfd)%(sawStep)).toInt()
        var fracInc = if(isSymmetricLfm) ((lfmDeviationPeriod*Fpfd)/(2*sawStep)).toInt()
        else ((lfmDeviationPeriod*Fpfd)/sawStep).toInt()

        if (fracIncRemain != 0) {
            fracInc += 1
            sawStep = if(isSymmetricLfm) ((lfmDeviationPeriod*Fpfd)/(2*fracInc)).toInt()
            else ((lfmDeviationPeriod*Fpfd)/(fracInc)).toInt()
        }

        return (sawStep shl 8) or fracInc or LFM2_REG
    }

    private fun getLfm1Register(
        lowestLfmFrequency: Long,
        highestLfmFrequency: Long,
        lfmDeviationPeriod: Double,
        isSymmetricLfm: Boolean
    ): Int {
        val Fpfd = Fref /getRefRegister(lfmDeviationPeriod, isSymmetricLfm)
        val deviationFreq = (highestLfmFrequency - lowestLfmFrequency)/4
        val sawStep = ((getLfm2Register(lfmDeviationPeriod, isSymmetricLfm)) and 0xFFFFF) shr 8
        val dfrac = (((deviationFreq)*16* MOD)/(sawStep*Fpfd)).toInt()

        return dfrac or LFM1_REG
    }

    private fun getLfm3Register(isSymmetricLfm: Boolean): Int {
        return if (isSymmetricLfm) LFM3
        else LFM3_NON_SYM
    }

    private fun getIntRegisterCw(frequency: Long): Int {
        return ((REF_REG_CW*frequency)/(4*Fref)).toInt()
    }

    private fun getRadiationMode(synthesizerModuleState: SynthesizerModuleState): RadiationMode {
        return when {
            synthesizerModuleState.lfm1Register[0] and 0xFFFFF != 0
                -> RadiationMode.LFM
            synthesizerModuleState.intRegister[0] in 2649..<2680
                -> RadiationMode.CW
            else -> RadiationMode.NONE
        }
    }

    private fun getCwRadiationFrequency(synthesizerModuleState: SynthesizerModuleState): Long {
        val refReg = synthesizerModuleState.refRegister[0]
        val intReg = synthesizerModuleState.intRegister[0]

        return (4*Fref*intReg)/refReg
    }

}