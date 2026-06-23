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
        //val testDispatcher = StandardTestDispatcher(testScheduler)
        val repository = PLLRegisters1208PL1URepositoryImpl()
        val result = repository.getLfmRegisters(
            LfmInputParametersModel(
                lowestLfmFrequency = 13_250_000_000,
                highestLfmFrequency = 13_400_000_000,
                lfmDeviationPeriod = 0.05,
                isSymmetricLfm = false
            )
        )
        val expectedRegisterList = arrayListOf(
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
            0x3FA0F9,
            0x500004,
            0x900000
        )

        assertThat(result).isEqualTo(expectedRegisterList)
    }
}