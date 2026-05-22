package ru.profitsw2000.data.model.pll

data class LfmInputParametersModel(
    val lowestLfmFrequency: Long,
    val highestLfmFrequency: Long,
    val lfmDeviationPeriod: Double,
    val isSymmetricLfm: Boolean
)
