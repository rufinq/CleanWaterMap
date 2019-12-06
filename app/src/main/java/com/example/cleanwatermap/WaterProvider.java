package com.example.cleanwatermap;

import org.threeten.bp.LocalDateTime;

import com.google.gson.annotations.SerializedName;

public class WaterProvider {
    private String id;
    @SerializedName("body")
    private LocalDateTime creationDate;
    private TDSMeasurement[] tdsMeasurements;
    private WaterProviderLocation waterProviderLocation;
    private byte[] photoData;

    public WaterProvider(TDSMeasurement[] tdsMeasurements, WaterProviderLocation waterProviderLocation, byte[] photoData) {
        this.tdsMeasurements = tdsMeasurements;
        this.waterProviderLocation = waterProviderLocation;
        this.photoData = photoData;
    }

    public WaterProvider(TDSMeasurement tdsMeasurements, WaterProviderLocation waterProviderLocation, byte[] photoData) {
        this.tdsMeasurements = new TDSMeasurement[1];
        this.tdsMeasurements[0] = tdsMeasurements;
        this.waterProviderLocation = waterProviderLocation;
        this.photoData = photoData;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public TDSMeasurement[] getTdsMeasurements() {
        return tdsMeasurements;
    }

    public void setTdsMeasurements(TDSMeasurement[] tdsMeasurements) {
        this.tdsMeasurements = tdsMeasurements;
    }

    public WaterProviderLocation getWaterProviderLocation() {
        return waterProviderLocation;
    }

    public void setWaterProviderLocation(WaterProviderLocation waterProviderLocation) {
        this.waterProviderLocation = waterProviderLocation;
    }

    public byte[] getPhotoData() {
        return photoData;
    }

    public void setPhotoData(byte[] photoData) {
        this.photoData = photoData;
    }
}
