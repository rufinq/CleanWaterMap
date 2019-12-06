package com.example.cleanwatermap;

import org.threeten.bp.LocalDateTime;

import com.google.gson.annotations.SerializedName;

public class WaterProvider {
    private String id;
    @SerializedName("body")
    private LocalDateTime creationDate;
    private TDSMeasurement[] tdsMeasurements;
    private WaterProviderLocation location;
    private String photoData; // base 64

    public WaterProvider(TDSMeasurement[] tdsMeasurements, WaterProviderLocation waterProviderLocation, String photoData) {
        this.tdsMeasurements = tdsMeasurements;
        this.location = waterProviderLocation;
        this.photoData = photoData;
    }

    public WaterProvider(TDSMeasurement tdsMeasurements, WaterProviderLocation waterProviderLocation, String photoData) {
        this.tdsMeasurements = new TDSMeasurement[1];
        this.tdsMeasurements[0] = tdsMeasurements;
        this.location = waterProviderLocation;
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
        return location;
    }

    public void setWaterProviderLocation(WaterProviderLocation aLocation) {
        this.location = aLocation;
    }

    public String getPhotoData() {
        return photoData;
    }

    public void setPhotoData(String photoData) {
        this.photoData = photoData;
    }
}
