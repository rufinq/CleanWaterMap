package com.example.cleanwatermap

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.os.Bundle
import android.os.Parcelable
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
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
            Timber.e("Could not find the photo from the intent")
        }
        return bitmap
    }

    private fun getByteArrayFromBitmap(aBitmap: Bitmap): ByteArray {
        val bmp: Bitmap? = retrievePhotoData()
        val stream = ByteArrayOutputStream()
        bmp?.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val byteArray: ByteArray = stream.toByteArray()
        bmp?.recycle()
        // TODO do I have to recycle here?
        return byteArray
    }

    fun convertByteArrayToBase64String(aByteArray : ByteArray): String {
        return Base64.encodeToString(aByteArray, Base64.DEFAULT)
    }

    fun createNewWaterLocationToAPI(aWaterProvider: WaterProvider) {
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

    fun getTDSValueFromTDSTextEdit() : Int {
        val theStringValue = TDSTextEdit.text.toString()
        if (theStringValue == "") {
            return 0
        }
        else {
            return TDSTextEdit.text.toString().toInt()
        }
    }

    fun addButtonPressed(view: android.view.View) {
        val photoData = retrievePhotoData()
        if (photoData == null) {
            Timber.e("photoData is null in addButtonPressed method")
        }
        val photoByteArray : ByteArray  = this.getByteArrayFromBitmap(photoData as Bitmap)
        val base64PhotoData = this.convertByteArrayToBase64String(photoByteArray)
        val aTDSMeasurement = TDSMeasurement(this.getTDSValueFromTDSTextEdit())
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location : Location? ->
                // Got last known location. In some rare situations this can be null.
                if (location != null) {
                    val latitude : Double = location.latitude
                    val longitude : Double = location.longitude
                    val aWaterProvider = WaterProvider(aTDSMeasurement, WaterProviderLocation(latitude, longitude), base64PhotoData)
                    this.createNewWaterLocationToAPI(aWaterProvider)
                }
                else {
                    Timber.e("location is null on 'addButtonPressed")
                }
            }
    }
}
