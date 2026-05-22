package ru.profitsw2000.data.domain.pll

import ru.profitsw2000.data.model.pll.LfmInputParametersModel

interface PLLRegisters1208PL1URepository {

    suspend fun getLfmRegisters(lfmInputParametersModel: LfmInputParametersModel): List<Int>

    suspend fun getCwRegisters(frequency: Long): List<Int>

}