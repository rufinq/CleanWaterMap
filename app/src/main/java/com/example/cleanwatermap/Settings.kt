package com.example.cleanwatermap

import android.location.Location

object Settings {

    private var filterData : FilterData = FilterData()

    val ignoreFilter : Boolean
        get() = filterData.ignoreFilter

    val userSpecifiedFilter : Boolean
        get() = !this.ignoreFilter

    var filterDistance : Int
        get() = filterData.distance

        set(distance : Int)  {
            assert(distance >= 0)
            filterData.distance = distance
        }

    var onlyTDSTestedWaterMachine : Boolean
        get() = filterData.onlyTDSTestedWaterMachine
        set(aBoolean: Boolean) {
            filterData.onlyTDSTestedWaterMachine = aBoolean
        }

    var onlySafeWaterMachine : Boolean
        get() = filterData.onlySafeWaterMachine
        set(aBoolean: Boolean) {
            filterData.onlySafeWaterMachine = aBoolean
        }

    fun matchFilterWithLocationAndWaterProvider(location : Location, aWaterProvider : WaterProvider) : Boolean {
        if (filterData.onlyTDSTestedWaterMachine && aWaterProvider.lastTDSMeasurementValue() == TDSMeasurement.UNTESTED_WATER_VALUE) return false
        if (filterData.onlySafeWaterMachine && aWaterProvider.lastTDSMeasurementValue() > TDSMeasurement.SAFE_TDS_VALUE_LIMIT) return false
        return if (filterData.ignoreDistance) true else aWaterProvider.distanceTo(location) < filterData.distance
    }
}