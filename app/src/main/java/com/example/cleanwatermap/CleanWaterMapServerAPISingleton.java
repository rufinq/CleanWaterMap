package com.example.cleanwatermap;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CleanWaterMapServerAPISingleton {

    private static volatile CleanWaterMapServerAPISingleton instance =
            new CleanWaterMapServerAPISingleton();

    private final String API_ADDRESS = "https://clean-water-map-server.herokuapp.com/";

    private CleanWaterMapServerAPI jsonPlaceHolderApi;

    //private constructor.
    private CleanWaterMapServerAPISingleton() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_ADDRESS)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        jsonPlaceHolderApi = retrofit.create(CleanWaterMapServerAPI.class);
    }

    private static CleanWaterMapServerAPISingleton getSingleton() {
        return instance;
    }

    public static CleanWaterMapServerAPI API() {
        return CleanWaterMapServerAPISingleton.getSingleton().jsonPlaceHolderApi;
    }
}
