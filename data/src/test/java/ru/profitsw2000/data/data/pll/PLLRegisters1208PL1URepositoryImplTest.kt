package ru.profitsw2000.data.data.pll

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import ru.profitsw2000.data.data.state.RchmDissStateRepositoryImpl
import ru.profitsw2000.data.model.bluetooth.state.rcd.TransmitterModuleState
import ru.profitsw2000.data.model.pll.LfmInputParametersModel
import ru.profitsw2000.data.model.rcd.RcdInputPacketType

@OptIn(ExperimentalCoroutinesApi::class)
class PLLRegisters1208PL1URepositoryImplTest {

    @Test
    fun `тест вычисления регистров ЛЧМ - 13250-13400, 50мс, НСМ`() = runTest {
        val repository = PLLRegisters1208PL1URepositoryImpl()
        val result = repository.getLfmRegisters(
            LfmInputParametersModel(
                lowestLfmFrequency = 13_250_000_000,
                highestLfmFrequency = 13_400_000_000,
                lfmDeviationPeriod = 0.05,
                isSymmetricLfm = false
            )
        )
        val expectedRegisterList = listOf(
            0x700000,
            0x000001,
            0x2000A5,
            0x404E20,
            0x607D00,
            0x840609,
            0x840608,
            0xA00002,
            0xC00001,
            0x1000F0,
            0x500004,
            0x3FA0F9,
            0x900000
        )

        assertThat(result).isEqualTo(expectedRegisterList)
    }

    @Test
    fun `тест вычисления регистров ЛЧМ - 13300-13400, 1мс, НСМ`() = runTest {
        val repository = PLLRegisters1208PL1URepositoryImpl()
        val result = repository.getLfmRegisters(
            LfmInputParametersModel(
                lowestLfmFrequency = 13_300_000_000,
                highestLfmFrequency = 13_400_000_000,
                lfmDeviationPeriod = 0.001,
                isSymmetricLfm = false
            )
        )
        val expectedRegisterList = listOf(
            0x700000,
            0x000001,
            0x2000A6,
            0x401F40,
            0x607D00,
            0x840609,
            0x840608,
            0xA00002,
            0xC00001,
            0x1000A0,
            0x500004,
            0x3FA004,
            0x900000
        )

        assertThat(result).isEqualTo(expectedRegisterList)
    }

    @Test
    fun `тест вычисления регистров ЛЧМ - 13250-13350, 0,2мс, НСМ`() = runTest {
        val repository = PLLRegisters1208PL1URepositoryImpl()
        val result = repository.getLfmRegisters(
            LfmInputParametersModel(
                lowestLfmFrequency = 13_250_000_000,
                highestLfmFrequency = 13_350_000_000,
                lfmDeviationPeriod = 0.0002,
                isSymmetricLfm = false
            )
        )
        val expectedRegisterList = listOf(
            0x700000,
            0x000001,
            0x2000A5,
            0x404E20,
            0x607D00,
            0x840609,
            0x840608,
            0xA00002,
            0xC00001,
            0x1000A0,
            0x500004,
            0x3FA000,
            0x900000
        )

        assertThat(result).isEqualTo(expectedRegisterList)
    }

    @Test
    fun `тест вычисления регистров ЛЧМ - 13250-13400, 50мс, СМ`() = runTest {
        val repository = PLLRegisters1208PL1URepositoryImpl()
        val result = repository.getLfmRegisters(
            LfmInputParametersModel(
                lowestLfmFrequency = 13_250_000_000,
                highestLfmFrequency = 13_400_000_000,
                lfmDeviationPeriod = 0.05,
                isSymmetricLfm = true
            )
        )
        val expectedRegisterList = listOf(
            0x700000,
            0x000001,
            0x2000A5,
            0x404E20,
            0x607D00,
            0x840609,
            0x840608,
            0xA00002,
            0xC00001,
            0x1000F0,
            0x500204,
            0x3FA07C,
            0x704000,
            0x000001,
            0x2000A7,
            0x403E80,
            0x607D00,
            0x840609,
            0x840608,
            0xA00002,
            0xC00001,
            0x1000F0,
            0x500006,
            0x3FA07C,
            0x900000
        )

        assertThat(result).isEqualTo(expectedRegisterList)
    }

    @Test
    fun `тест вычисления регистров ЛЧМ - 13300-13400, 1мс, СМ`() = runTest {
        val repository = PLLRegisters1208PL1URepositoryImpl()
        val result = repository.getLfmRegisters(
            LfmInputParametersModel(
                lowestLfmFrequency = 13_300_000_000,
                highestLfmFrequency = 13_400_000_000,
                lfmDeviationPeriod = 0.001,
                isSymmetricLfm = true
            )
        )
        val expectedRegisterList = listOf(
            0x700000,
            0x000001,
            0x2000A6,
            0x401F40,
            0x607D00,
            0x840609,
            0x840608,
            0xA00002,
            0xC00001,
            0x1000C0,
            0x500204,
            0x3D0502,
            0x704000,
            0x000001,
            0x2000A7,
            0x403E80,
            0x607D00,
            0x840609,
            0x840608,
            0xA00002,
            0xC00001,
            0x1000C0,
            0x500006,
            0x3D0502,
            0x900000
        )

        assertThat(result).isEqualTo(expectedRegisterList)
    }

    @Test
    fun `тест вычисления регистров ЛЧМ - 13250-13350, 0,2мс, СМ`() = runTest {
        val repository = PLLRegisters1208PL1URepositoryImpl()
        val result = repository.getLfmRegisters(
            LfmInputParametersModel(
                lowestLfmFrequency = 13_250_000_000,
                highestLfmFrequency = 13_350_000_000,
                lfmDeviationPeriod = 0.0002,
                isSymmetricLfm = true
            )
        )
        val expectedRegisterList = listOf(
            0x700000,
            0x000001,
            0x2000A5,
            0x404E20,
            0x607D00,
            0x840609,
            0x840608,
            0xA00002,
            0xC00001,
            0x100140,
            0x500204,
            0x37D000,
            0x704000,
            0x000001,
            0x2000A6,
            0x406D60,
            0x607D00,
            0x840609,
            0x840608,
            0xA00002,
            0xC00001,
            0x100140,
            0x500006,
            0x37D000,
            0x900000
        )

        assertThat(result).isEqualTo(expectedRegisterList)
    }

    @Test
    fun `тест вычисления регистров ЛЧМ - 13280-13350, 0,3мс, СМ`() = runTest {
        val repository = PLLRegisters1208PL1URepositoryImpl()
        val result = repository.getLfmRegisters(
            LfmInputParametersModel(
                lowestLfmFrequency = 13_280_000_000,
                highestLfmFrequency = 13_350_000_000,
                lfmDeviationPeriod = 0.0003,
                isSymmetricLfm = true
            )
        )
        val expectedRegisterList = listOf(
            0x700000,
            0x000001,
            0x2000A6,
            0x400000,
            0x607D00,
            0x840609,
            0x840608,
            0xA00002,
            0xC00001,
            0x100095,
            0x500204,
            0x3BB700,
            0x704000,
            0x000001,
            0x2000A6,
            0x406D60,
            0x607D00,
            0x840609,
            0x840608,
            0xA00002,
            0xC00001,
            0x100095,
            0x500006,
            0x3BB700,
            0x900000
        )

        assertThat(result).isEqualTo(expectedRegisterList)
    }

    @Test
    fun `тест вычисления регистров ЛЧМ - 13320-13400, 0,4мс, СМ`() = runTest {
        val repository = PLLRegisters1208PL1URepositoryImpl()
        val result = repository.getLfmRegisters(
            LfmInputParametersModel(
                lowestLfmFrequency = 13_320_000_000,
                highestLfmFrequency = 13_400_000_000,
                lfmDeviationPeriod = 0.0004,
                isSymmetricLfm = true
            )
        )
        val expectedRegisterList = listOf(
            0x700000,
            0x000001,
            0x2000A6,
            0x403E80,
            0x607D00,
            0x840609,
            0x840608,
            0xA00002,
            0xC00001,
            0x100080,
            0x500204,
            0x3FA000,
            0x704000,
            0x000001,
            0x2000A7,
            0x403E80,
            0x607D00,
            0x840609,
            0x840608,
            0xA00002,
            0xC00001,
            0x100080,
            0x500006,
            0x3FA000,
            0x900000
        )

        assertThat(result).isEqualTo(expectedRegisterList)
    }

    @Test
    fun `тест вычисления регистров ЛЧМ - 13300-13375, 0,5мс, СМ`() = runTest {
        val repository = PLLRegisters1208PL1URepositoryImpl()
        val result = repository.getLfmRegisters(
            LfmInputParametersModel(
                lowestLfmFrequency = 13_300_000_000,
                highestLfmFrequency = 13_375_000_000,
                lfmDeviationPeriod = 0.0005,
                isSymmetricLfm = true
            )
        )
        val expectedRegisterList = listOf(
            0x700000,
            0x000001,
            0x2000A6,
            0x401F40,
            0x607D00,
            0x840609,
            0x840608,
            0xA00002,
            0xC00001,
            0x1000C0,
            0x500204,
            0x39C401,
            0x704000,
            0x000001,
            0x2000A7,
            0x401770,
            0x607D00,
            0x840609,
            0x840608,
            0xA00002,
            0xC00001,
            0x1000C0,
            0x500006,
            0x39C401,
            0x900000
        )

        assertThat(result).isEqualTo(expectedRegisterList)
    }

    @Test
    fun `тест вычисления регистров ЛЧМ - 13250-13400, 0,7мс, СМ`() = runTest {
        val repository = PLLRegisters1208PL1URepositoryImpl()
        val result = repository.getLfmRegisters(
            LfmInputParametersModel(
                lowestLfmFrequency = 13_250_000_000,
                highestLfmFrequency = 13_400_000_000,
                lfmDeviationPeriod = 0.0007,
                isSymmetricLfm = true
            )
        )
        val expectedRegisterList = listOf(
            0x700000,
            0x000001,
            0x2000A5,
            0x404E20,
            0x607D00,
            0x840609,
            0x840608,
            0xA00002,
            0xC00001,
            0x100112,
            0x500204,
            0x3DAC01,
            0x704000,
            0x000001,
            0x2000A7,
            0x403E80,
            0x607D00,
            0x840609,
            0x840608,
            0xA00002,
            0xC00001,
            0x100112,
            0x500006,
            0x3DAC01,
            0x900000
        )

        assertThat(result).isEqualTo(expectedRegisterList)
    }
}