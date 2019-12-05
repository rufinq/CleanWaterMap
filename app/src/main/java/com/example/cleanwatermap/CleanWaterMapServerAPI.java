package com.example.cleanwatermap;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

interface CleanWaterMapServerAPI {
    @GET("waterProvider")
    Call<List<WaterProvider>> getWaterProviders();

    @POST("waterProvider")
    Call<WaterProvider> createWaterProvider(@Body WaterProvider waterProvider);
}