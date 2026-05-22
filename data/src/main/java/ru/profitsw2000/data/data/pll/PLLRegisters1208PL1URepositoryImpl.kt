package ru.profitsw2000.data.data.pll

import ru.profitsw2000.data.domain.pll.PLLRegisters1208PL1URepository
import ru.profitsw2000.data.model.pll.LfmInputParametersModel

const val MOD = 32_000
const val Fref = 20_000_000L
const val CTR1_RST = 0x840609
const val CTR1 = 0x840608
const val CTR2 = 0xA00002
const val CTR3 = 0xC00001
const val LFM3 = 0x500204
const val LFM31 = 0x500006
const val INT_REG = 0x200000
const val FRAC_REG = 0x400000
const val LFM1_REG = 0x100000
const val LFM2_REG = 0x300000
const val MOD_REG = 0x607D00

class PLLRegisters1208PL1URepositoryImpl : PLLRegisters1208PL1URepository {

    override suspend fun getLfmRegisters(lfmInputParametersModel: LfmInputParametersModel): List<Int> {
        val registersList = mutableListOf<Int>()

        with(lfmInputParametersModel) {
            registersList.add(getRefRegister(lfmDeviationPeriod, isSymmetricLfm))
            registersList.add(getIntRegister(lowestLfmFrequency, lfmDeviationPeriod, isSymmetricLfm))
            registersList.add(getFracRegister(lowestLfmFrequency, lfmDeviationPeriod, isSymmetricLfm))
            registersList.add(MOD_REG)
            registersList.add(CTR1_RST)
            registersList.add(CTR1)
            registersList.add(CTR2)
            registersList.add(CTR3)
            registersList.add(getLfm1Register(lowestLfmFrequency, highestLfmFrequency, lfmDeviationPeriod, isSymmetricLfm))
            registersList.add(getLfm3Register(isSymmetricLfm))
            registersList.add(getLfm2Register(lfmDeviationPeriod, isSymmetricLfm))
        }
        return registersList
    }

    override suspend fun getCwRegisters(frequency: Long): List<Int> {
        TODO("Not yet implemented")
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
        else LFM31
    }

}