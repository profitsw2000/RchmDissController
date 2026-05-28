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
const val REGISTERS_VALUE_MASK = 0xFFFFF

class PLLRegisters1208PL1URepositoryImpl : PLLRegisters1208PL1URepository {

    /**
     * Асинхронная функция для получения списка типа Int для записи в регистры микросхемы 1288ПЛ1У
     * @param lfmInputParametersModel - содержит в себе параметры необходимого сигнала ЛЧМ для генерации микросхемой (начальное
     * значение частоты, конечное значение частоты, период девиации и т.д.)
     * @return список с числами типа Int, где каждый элемент содержит значение регистра
     */
    override suspend fun getLfmRegisters(lfmInputParametersModel: LfmInputParametersModel): List<Int> {
        return buildList {
            addAll(getLfmRegistersFirstProfile(lfmInputParametersModel))
            addAll(getLfmRegistersSecondProfile(lfmInputParametersModel))
            add(PRA0_REG)
        }
    }

    /**
     * Асинхронная функция для получения списка типа Int для записи в регистры микросхемы 1288ПЛ1У
     * @param frequency - содержит в себе частоту немодулированного сигнал для генерации микросхемой
     * @return список с числами типа Int, где каждый элемент содержит значение регистра
     */
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

    /**
     * Функция для получения значений параметров синтезатора - тип генерации сигнала (НГ или ЛЧМ), частоты
     * период и пр.
     * @param synthesizerModuleState - содержит в себе текущее значение регистров микросхемы 1288ПЛ1У для двух профилей
     * @return объект типа SynthesizerModuleStateModel с текущими параметрами сигнала, рассчитаными на основе значений регистров
     */
    override suspend fun getLfmParameters(synthesizerModuleState: SynthesizerModuleState): SynthesizerModuleStateModel {
        val radiationMode = getRadiationMode(synthesizerModuleState)

        return when(radiationMode) {
            RadiationMode.NONE -> SynthesizerModuleStateModel()
            RadiationMode.CW -> cwSynthesizerParametersModel(synthesizerModuleState)
            RadiationMode.LFM -> lfmSynthesizerParametersModel(synthesizerModuleState)
        }
    }

    /**
     * Функция для получения списка со значениями регистров для первого профиля м/сх 1288ПЛ1У для генерации ЛЧМ сигнала.
     * Элемент списка типа Int
     * содержит 4 байта, регистр м/сх 1288ПЛ1У использует только 3 первых байта. Тип регистра закодирован в последних
     * четырёх битах регистра.
     * @param lfmInputParametersModel - содержит параметры ЛЧМ сигнала
     * @return список из регистров микросхемы 1288ПЛ1У для записи в первый профиль
     */
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

    /**
     * Функция для получения списка со значениями регистров для второго профиля м/сх 1288ПЛ1У для генерации ЛЧМ сигнала.
     * Элемент списка типа Int
     * содержит 4 байта, регистр м/сх 1288ПЛ1У использует только 3 первых байта. Тип регистра закодирован в последних
     * четырёх битах регистра.
     * @param lfmInputParametersModel - содержит параметры ЛЧМ сигнала
     * @return список из регистров микросхемы 1288ПЛ1У для записи во второй профиль
     */
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

    /**
     * Рассчитывает значение регистра Ref микросхемы ФАПЧ
     * @param lfmDeviationPeriod - содержит период модуляции ЛЧМ сигнала
     * @param isSymmetricLfm - булевая переменная по которой определяется тип сигнала ЛЧМ - симметричный или несимметричный
     * @return Значение регистра Ref, находящийся в переменной Int (используются только 3 первых байта)
     */
    private fun getRefRegister(
        lfmDeviationPeriod: Double,
        isSymmetricLfm: Boolean
    ): Int {
        return if (lfmDeviationPeriod <= 0.05 || isSymmetricLfm) 1
        else 2
    }

    /**
     * Рассчитывает значение регистра Int микросхемы ФАПЧ для первого профиля
     * @param lowestLfmFrequency - начальное значение частоты ЛЧМ сигнала
     * @param lfmDeviationPeriod - содержит период модуляции ЛЧМ сигнала
     * @param isSymmetricLfm - булевая переменная по которой определяется тип сигнала ЛЧМ - симметричный или несимметричный
     * @return Значение регистра Int первого профиля, находящийся в переменной типа Int (используются только 3 первых байта)
     */
    private fun getIntRegister(
        lowestLfmFrequency: Long,
        lfmDeviationPeriod: Double,
        isSymmetricLfm: Boolean
    ): Int {
        val ref = getRefRegister(lfmDeviationPeriod, isSymmetricLfm)
        return (((lowestLfmFrequency*ref)/(4* Fref)).toInt()) or INT_REG
    }

    /**
     * Рассчитывает значение регистра Int микросхемы ФАПЧ для второго профиля
     * @param highestLfmFrequency - конечное значение частоты ЛЧМ сигнала
     * @param lfmDeviationPeriod - содержит период модуляции ЛЧМ сигнала
     * @param isSymmetricLfm - булевая переменная по которой определяется тип сигнала ЛЧМ - симметричный или несимметричный
     * @return Значение регистра Int второго профиля, находящийся в переменной типа Int (используются только 3 первых байта)
     */
    private fun getInt1Register(
        highestLfmFrequency: Long,
        lfmDeviationPeriod: Double,
        isSymmetricLfm: Boolean
    ): Int {
        val ref = getRefRegister(lfmDeviationPeriod, isSymmetricLfm)
        return (((highestLfmFrequency*ref)/(4* Fref)).toInt()) or INT_REG
    }

    /**
     * Рассчитывает значение регистра Frac микросхемы ФАПЧ для первого профиля
     * @param lowestLfmFrequency - начальное значение частоты ЛЧМ сигнала
     * @param lfmDeviationPeriod - содержит период модуляции ЛЧМ сигнала
     * @param isSymmetricLfm - булевая переменная по которой определяется тип сигнала ЛЧМ - симметричный или несимметричный
     * @return Значение регистра Frac первого профиля, находящийся в переменной типа Int (используются только 3 первых байта)
     */
    private fun getFracRegister(
        lowestLfmFrequency: Long,
        lfmDeviationPeriod: Double,
        isSymmetricLfm: Boolean
    ): Int {
        val ref = getRefRegister(lfmDeviationPeriod, isSymmetricLfm)
        val fractionalMultPart = (lowestLfmFrequency*ref)%(4* Fref)
        return (((MOD *ref*fractionalMultPart)/(4* Fref)).toInt()) or FRAC_REG
    }

    /**
     * Рассчитывает значение регистра Frac микросхемы ФАПЧ для второго профиля
     * @param highestLfmFrequency - конечное значение частоты ЛЧМ сигнала
     * @param lfmDeviationPeriod - содержит период модуляции ЛЧМ сигнала
     * @param isSymmetricLfm - булевая переменная по которой определяется тип сигнала ЛЧМ - симметричный или несимметричный
     * @return Значение регистра Frac второго профиля, находящийся в переменной типа Int (используются только 3 первых байта)
     */
    private fun getFrac1Register(
        highestLfmFrequency: Long,
        lfmDeviationPeriod: Double,
        isSymmetricLfm: Boolean
    ): Int {
        val ref = getRefRegister(lfmDeviationPeriod, isSymmetricLfm)
        val fractionalMultPart = (highestLfmFrequency*ref)%(4* Fref)
        return (((MOD *ref*fractionalMultPart)/(4* Fref)).toInt()) or FRAC_REG
    }

    /**
     * Рассчитывает значение регистра Lfm2 микросхемы ФАПЧ для обоих профилей
     * @param lfmDeviationPeriod - содержит период модуляции ЛЧМ сигнала
     * @param isSymmetricLfm - булевая переменная по которой определяется тип сигнала ЛЧМ - симметричный или несимметричный
     * @return Значение регистра Lfm2 обоих профилей, находящийся в переменной типа Int (используются только 3 первых байта)
     */
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

    /**
     * Рассчитывает значение регистра Lfm1 микросхемы ФАПЧ для обоих профилей
     * @param lowestLfmFrequency - начальное значение частоты ЛЧМ сигнала
     * @param highestLfmFrequency - конечное значение частоты ЛЧМ сигнала
     * @param lfmDeviationPeriod - содержит период модуляции ЛЧМ сигнала
     * @param isSymmetricLfm - булевая переменная по которой определяется тип сигнала ЛЧМ - симметричный или несимметричный
     * @return Значение регистра Lfm1 обоих профилей, находящийся в переменной типа Int (используются только 3 первых байта)
     */
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

    /**
     * Возвращает значение регистра Lfm3 микросхемы ФАПЧ для первого профиля
     * @param isSymmetricLfm - булевая переменная по которой определяется тип сигнала ЛЧМ - симметричный или несимметричный
     * @return Значение регистра Lfm3 первого профиля, находящийся в переменной типа Int (используются только 3 первых байта)
     */
    private fun getLfm3Register(isSymmetricLfm: Boolean): Int {
        return if (isSymmetricLfm) LFM3
        else LFM3_NON_SYM
    }

    /**
     * Рассчитывает значение регистра Int микросхемы ФАПЧ для генерации немодулированного сигнала(НГ)
     * @param frequency - значение частоты сигнала
     * @return Значение регистра Int, находящийся в переменной типа Int (используются только 3 первых байта)
     */
    private fun getIntRegisterCw(frequency: Long): Int {
        return ((REF_REG_CW*frequency)/(4*Fref)).toInt()
    }

    /**
     * Функция определяет по значениям регистров Lfm1 и Int первого профиля ФАПЧ какой тип сигнала будет
     * генерироваться
     * @param synthesizerModuleState - содержит в себе текущее значение регистров микросхемы 1288ПЛ1У для двух профилей
     * @return объект типа RadiationMode, который содержит информацию о виде генерируемого сигнала(НГ или ЛЧМ)
     */
    private fun getRadiationMode(synthesizerModuleState: SynthesizerModuleState): RadiationMode {
        return when {
            (synthesizerModuleState.lfm1Register[0] and REGISTERS_VALUE_MASK) != 0
                -> RadiationMode.LFM
            (synthesizerModuleState.intRegister[0] and REGISTERS_VALUE_MASK) in 2649..<2680
                -> RadiationMode.CW
            else -> RadiationMode.NONE
        }
    }

    /**
     * Рассчитывает значение параметров НГ сигнала и помещает их в объект типа SynthesizerModuleStateModel
     * @param synthesizerModuleState - переменная со значениями регистров микросхемы ФАПЧ
     * @return объект типа SynthesizerModuleStateModel с параметрами НГ сигнала
     */
    private fun cwSynthesizerParametersModel(synthesizerModuleState: SynthesizerModuleState): SynthesizerModuleStateModel {
        return SynthesizerModuleStateModel(
            radiationMode = RadiationMode.CW,
            cwFrequency = getCwRadiationFrequency(synthesizerModuleState)
        )
    }

    /**
     * Рассчитывает значение параметров ЛЧМ сигнала и помещает их в объект типа SynthesizerModuleStateModel
     * @param synthesizerModuleState - переменная со значениями регистров микросхемы ФАПЧ
     * @return объект типа SynthesizerModuleStateModel с параметрами ЛЧМ сигнала
     */
    private fun lfmSynthesizerParametersModel(synthesizerModuleState: SynthesizerModuleState): SynthesizerModuleStateModel {
        return SynthesizerModuleStateModel(
            radiationMode = RadiationMode.LFM,
            lowestLfmFrequency = getLfmLowestFrequency(synthesizerModuleState),
            highestLfmFrequency = getLfmHighestFrequency(synthesizerModuleState),
            lfmPeriod = getLfmDeviationPeriod(synthesizerModuleState),
            isSymmetricLfm = isSymmetricLfm(synthesizerModuleState.lfm3Register[0])
        )
    }

    /**
     * Функция рассчитывает значение частоты НГ сигнала по значениям регистров
     * @param synthesizerModuleState - аргумент, в котором содержаться значения регистров микросхемы ФАПЧ
     * @return значение частоты в переменной типа Long
     */
    private fun getCwRadiationFrequency(synthesizerModuleState: SynthesizerModuleState): Long {
        val refReg = (synthesizerModuleState.refRegister[0] and REGISTERS_VALUE_MASK).toLong()
        val intReg = (synthesizerModuleState.intRegister[0] and REGISTERS_VALUE_MASK).toLong()

        return (4*Fref*intReg)/refReg
    }

    /**
     * Определение типа ЛЧМ-сигнала (симметричный или несимметричный) по значениям регистров микросхемы ФАПЧ
     * @param register - аргумент, в котором содержится значение регистра микросхемы ФАПЧ по которому определяется тип сигнала
     * @return булевая переменная - если true - сигнал, генерируемый микросхемой ФАПЧ - симметричный, false - несимметричный.
     */
    private fun isSymmetricLfm(register: Int): Boolean {
        return (register.shr(9) and 0x1F) != 0
    }

    /**
     * Рассчитывает нижнее значение частоты ЛЧМ-сигнала по значениям регистров
     * @param synthesizerModuleState - аргумент, в котором содержаться значения регистров микросхемы ФАПЧ
     * @return нижнее значение частоты в переменной типа Long
     */
    private fun getLfmLowestFrequency(
        synthesizerModuleState: SynthesizerModuleState
    ): Long {
        val intReg = (synthesizerModuleState.intRegister[0] and REGISTERS_VALUE_MASK).toLong()
        val fracReg = (synthesizerModuleState.fracRegister[0] and REGISTERS_VALUE_MASK).toLong()
        val modReg = (synthesizerModuleState.modRegister[0] and REGISTERS_VALUE_MASK).toLong()
        val refReg = (synthesizerModuleState.refRegister[0] and REGISTERS_VALUE_MASK).toLong()

        return (4*Fref*(intReg + (fracReg/modReg)))/refReg
    }

    /**
     * Рассчитывает верхнее значение частоты ЛЧМ-сигнала по значениям регистров. При этом
     * для случаев симметричной и несимметричной ЛЧМ значение расчитывается по разному.
     * @param synthesizerModuleState - аргумент, в котором содержаться значения регистров микросхемы ФАПЧ
     * @return верхнее значение частоты в переменной типа Long
     */
    private fun getLfmHighestFrequency(
        synthesizerModuleState: SynthesizerModuleState
    ): Long {
        val intReg = (synthesizerModuleState.intRegister[1] and REGISTERS_VALUE_MASK).toLong()
        val fracReg = (synthesizerModuleState.fracRegister[1] and REGISTERS_VALUE_MASK).toLong()
        val modReg = (synthesizerModuleState.modRegister[1] and REGISTERS_VALUE_MASK).toLong()
        val refReg = (synthesizerModuleState.refRegister[1] and REGISTERS_VALUE_MASK).toLong()
        val dFracReg = (synthesizerModuleState.lfm1Register[0] and REGISTERS_VALUE_MASK).toLong()

        return if(isSymmetricLfm(synthesizerModuleState.lfm3Register[0])) (4*Fref*(intReg + (fracReg/modReg)))/refReg
        else getLfmHighestFrequency(synthesizerModuleState) + ((dFracReg*Fref)/(16*modReg*refReg))
    }

    /**
     * Рассчитывает период модуляции частоты ЛЧМ-сигнала по значениям регистров. При этом
     * для случаев симметричной и несимметричной ЛЧМ значение расчитывается по разному.
     * @param synthesizerModuleState - аргумент, в котором содержаться значения регистров микросхемы ФАПЧ
     * @return период модуляции в переменной типа Double
     */
    private fun getLfmDeviationPeriod(synthesizerModuleState: SynthesizerModuleState): Double {
        val fracInc = ((synthesizerModuleState.lfm2Register[0] and REGISTERS_VALUE_MASK) and 0xFF).toDouble()
        val sawStep = ((synthesizerModuleState.lfm2Register[0] and REGISTERS_VALUE_MASK).shr(8)).toLong()
        val refReg = (synthesizerModuleState.refRegister[0] and REGISTERS_VALUE_MASK).toLong()
        val deviationTime = fracInc*((refReg*sawStep)/Fref)

        return if(isSymmetricLfm(synthesizerModuleState.lfm3Register[0])) deviationTime*2
        else deviationTime
    }

}