package com.example.cleanwatermap

import android.os.Parcel
import android.os.Parcelable

data class FilterData(
    var meters: Int = Int.MAX_VALUE,
    var onlyTDSTestedWaterMachine : Boolean = false,
    var onlySafeWaterMachine : Boolean = false) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte()
    )

    fun ignoreDistance() : Boolean {
        return meters == Int.MAX_VALUE
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(meters)
        parcel.writeByte(if (onlyTDSTestedWaterMachine) 1 else 0)
        parcel.writeByte(if (onlySafeWaterMachine) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<FilterData> {
        override fun createFromParcel(parcel: Parcel): FilterData {
            return FilterData(parcel)
        }

        override fun newArray(size: Int): Array<FilterData?> {
            return arrayOfNulls(size)
        }
    }
}