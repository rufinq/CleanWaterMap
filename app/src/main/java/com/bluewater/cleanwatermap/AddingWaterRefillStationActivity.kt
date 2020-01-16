package com.bluewater.cleanwatermap

import android.app.AlertDialog
import android.graphics.Bitmap
import android.location.Location
import android.os.Bundle
import android.os.Parcelable
import android.util.Base64
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import es.dmoral.toasty.Toasty
import retrofit2.Response
import timber.log.Timber
import java.io.ByteArrayOutputStream


class AddingWaterRefillStationActivity : AppCompatActivity() {

    private lateinit  var mWaterRefillWaterPhoto : ImageView
    private lateinit var mAddressContent : TextView
    private lateinit var mTDSTextEdit : EditText
    private lateinit var mAddButton : Button

    private lateinit var mFusedLocationClient: FusedLocationProviderClient


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adding_water_refill_station)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        performLateInitView()
        updatePhotoFromIntent()
    }

    private fun performLateInitView() {
        mWaterRefillWaterPhoto = findViewById(R.id.refillStationPhoto)
        mAddressContent = findViewById(R.id.addressContent)
        mTDSTextEdit = findViewById(R.id.tdsValue)
        mAddButton = findViewById(R.id.addButton)
    }

    private fun updatePhotoFromIntent() {
        mWaterRefillWaterPhoto.setImageBitmap(retrievePhotoData())
    }

    private fun retrievePhotoData() : Bitmap? {
        val aParcelable : Parcelable? = intent.getParcelableExtra("data")
        if (aParcelable != null) {
            return aParcelable as Bitmap
        }
        else
        {
            Timber.e("Could not find the photo from the intent")
        }
        return null
    }

    private fun getByteArrayFromBitmap(aBitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        aBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val byteArray: ByteArray = stream.toByteArray()
//        aBitmap.recycle()
        return byteArray
    }

    private fun convertByteArrayToBase64String(aByteArray : ByteArray): String {
        return Base64.encodeToString(aByteArray, Base64.DEFAULT)
    }

    private fun createNewWaterLocationToAPI(aWaterProvider: WaterProvider) {
        CleanWaterMapServerAPISingleton.API().createWaterProvider(aWaterProvider).enqueue {
            onResponse = {response : Response<WaterProvider>? ->
                if(response != null && response.isSuccessful) {
                    finish()
                }
            }
            onFailure = {
                Toasty.warning(baseContext, "Unable to connect to server").show()
            }
        }
    }

    private fun getTDSValueString() : String {
        return mTDSTextEdit.text.toString()
    }

    private fun hasUserSpecifiedA0TDSValue() : Boolean {
        val stringTextEdit : String =  this.getTDSValueString()

        return stringTextEdit != "" && stringTextEdit.toInt() == 0
    }

    private fun modifyTDSValueForSpecialCases(aWaterProvider: WaterProvider) {
        val tdsValueString = this.getTDSValueString()
        val number : Int = when (tdsValueString) {
            "" -> TDSMeasurement.UNTESTED_WATER_VALUE
            "0" -> 1
            else -> {
                tdsValueString.toInt()
            }
        }
        aWaterProvider.setLastTDSMeasurementValue(number)
    }

    private fun showConfirmationDialog(yesClicked : () -> Unit, noClicked : () -> Unit) {
        // TODO internationalization here
        val confirmationDialogTitle = "TDS Value"
        val messageDialog = "Are you SURE the TDS Meter touched the water?"

        AlertDialog.Builder(this)
            .setTitle(confirmationDialogTitle)
            .setMessage(messageDialog)
            .setPositiveButton(android.R.string.yes) { _, _ -> yesClicked() }
            .setNegativeButton(android.R.string.no) { _, _ -> noClicked() }
            .show()
    }

    private fun getTDSValueFromTDSTextEdit() : Int {
        val theStringValue = mTDSTextEdit.text.toString()
        var returnValue = TDSMeasurement.UNTESTED_WATER_VALUE
        if (theStringValue != "") {
            returnValue = theStringValue.toInt()
        }
        return returnValue
    }

    fun addButtonPressed(@Suppress("UNUSED_PARAMETER") view: android.view.View) {
        val photoData = retrievePhotoData()
        if (photoData == null) {
            Timber.e("photoData is null in addButtonPressed method")
        }
        val photoByteArray : ByteArray  = this.getByteArrayFromBitmap(photoData as Bitmap)
        val base64PhotoData = this.convertByteArrayToBase64String(photoByteArray)
        val tdsValue : Int = this.getTDSValueFromTDSTextEdit()
        val aTDSMeasurement = TDSMeasurement(tdsValue)
        mFusedLocationClient.lastLocation
            .addOnSuccessListener {location : Location? ->
                // Got last known location. In some rare situations this can be null.
                if (location != null) {
                    val latitude : Double = location.latitude
                    val longitude : Double = location.longitude
                    val aWaterProvider = WaterProvider(aTDSMeasurement, WaterProviderLocation(latitude, longitude), base64PhotoData)
                    var confirmationDialogHasShownAndUserClickedNo = false
                    if (this.hasUserSpecifiedA0TDSValue()) {
                        this.showConfirmationDialog(yesClicked = {

                        }, noClicked =  {
                            confirmationDialogHasShownAndUserClickedNo = true
                        })
                    }
                    if (!confirmationDialogHasShownAndUserClickedNo) {
                        this.modifyTDSValueForSpecialCases(aWaterProvider)
                        this.createNewWaterLocationToAPI(aWaterProvider)
                    }
                }
                else {
                    Timber.e("location is null on 'addButtonPressed")
                }
            }
    }
}
