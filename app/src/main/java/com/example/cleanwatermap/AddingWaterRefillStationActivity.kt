package com.example.cleanwatermap

import android.graphics.Bitmap
import android.location.Location
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.android.synthetic.main.activity_adding_water_refill_station.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream


class AddingWaterRefillStationActivity : AppCompatActivity() {

    private lateinit  var waterRefillWaterPhoto : ImageView
    private lateinit var addressContent : TextView
    private lateinit var TDSTextEdit : EditText
    private lateinit var addButon : Button

    private lateinit var fusedLocationClient: FusedLocationProviderClient


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adding_water_refill_station)
        // TODO is this at the right place?
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        performLateInitView()
        updatePhotoFromIntent()
    }

    private fun performLateInitView() {
        waterRefillWaterPhoto = findViewById(R.id.refillStationPhoto)
        addressContent = findViewById(R.id.addressContent)
        TDSTextEdit = findViewById(R.id.tdsValue)
        addButon = findViewById(R.id.addButton)
    }

    private fun updatePhotoFromIntent() {
        waterRefillWaterPhoto.setImageBitmap(retrievePhotoData())
    }

    private fun retrievePhotoData() : Bitmap? {
        val aParcelable : Parcelable? = intent.getParcelableExtra("data")
        var bitmap = null
        if (aParcelable != null) {
            return aParcelable as Bitmap
        }
        else
        {
            Log.e("retrievePhotoData", "Could not find the photo from the intent")
        }
        return bitmap
    }

    private fun getByteArrayFromBitmap(aBitmap: Bitmap?): ByteArray {
        val bmp: Bitmap? = retrievePhotoData()
        val stream = ByteArrayOutputStream()
        bmp!!.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val byteArray: ByteArray = stream.toByteArray()
//        bmp.recycle()
        // TODO do I have to recycle here?
        return byteArray
    }

    fun addButtonPressed(view: android.view.View) {
        val photoByteArray : ByteArray  = getByteArrayFromBitmap(retrievePhotoData())
        val aTDSMeasurement = TDSMeasurement(TDSTextEdit.text.toString().toInt())
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location : Location? ->
                // Got last known location. In some rare situations this can be null.
                if (location != null) {
                    val latitude : Double = location.latitude
                    val longitude : Double = location.longitude
                    val aWaterProvider = WaterProvider(aTDSMeasurement, WaterProviderLocation(latitude, longitude), photoByteArray)
                    var waterProviderCall : Call<WaterProvider> = CleanWaterMapServerAPISingleton.API().createWaterProvider(aWaterProvider)
                    waterProviderCall.enqueue(object : Callback<WaterProvider> {

                        override fun onResponse(call: Call<WaterProvider>?, response: Response<WaterProvider>?) {
                            if(response != null && response.isSuccessful) {
                                finish()
                            }
                        }

                        override fun onFailure(call: Call<WaterProvider>?, t: Throwable?) {

                        }
                    })

                }
                else {
                    Log.e("addButtonPressed", "location is null on 'addButtonPressed'")
                }
            }
    }
}
