package com.example.cleanwatermap;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.annotations.SerializedName;

class WaterProviderLocation {
    @SerializedName("body")
    private double latitude;

    private double longitude;

    public WaterProviderLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public LatLng convertToLatLng() {
        return new LatLng(latitude, longitude);
    }
}