package com.example.cleanwatermap;

import com.google.gson.annotations.SerializedName;

import org.threeten.bp.LocalDateTime;

public class TDSMeasurement {
    String id;
    @SerializedName("body")
    LocalDateTime date;
    String deviceName;
    int tdsValue;

    static String defaultDeviceName = "TDS-3";

    public TDSMeasurement(int tdsValue) {
        this.tdsValue = tdsValue;
        this.date = LocalDateTime.now();
        this.deviceName = defaultDeviceName;
    }
}