package com.example.cleanwatermap

data class FilterData(
    var distance: Int = Int.MAX_VALUE,
    var onlyTDSTestedWaterMachine : Boolean = false,
    var onlySafeWaterMachine : Boolean = false) {

    val ignoreDistance : Boolean
        get() = distance == Int.MAX_VALUE

    val ignoreFilter : Boolean
        get() = ignoreDistance && !onlySafeWaterMachine && !onlyTDSTestedWaterMachine

}