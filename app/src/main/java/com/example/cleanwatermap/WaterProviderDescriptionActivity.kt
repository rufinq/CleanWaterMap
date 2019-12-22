package com.example.cleanwatermap

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.justinnguyenme.base64image.Base64Image


class WaterProviderDescriptionActivity : AppCompatActivity() {

    companion object {
        const val WATER_PROVIDER_INTENT_DATA_KEY : String = "waterProviderData"
    }

    private lateinit var mWaterProviderWaterPhoto : ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_water_provider_description)
        this.performLateInitView()
        this.updateWaterProviderPhotoFromIntent()
    }

    private fun performLateInitView() {
        mWaterProviderWaterPhoto = findViewById(R.id.waterProviderImage)
    }

    private fun updateWaterProviderPhotoFromIntent() {
        val waterProviderData : WaterProvider? = this.retrieveWaterProviderData()
        if (waterProviderData != null) {
            Base64Image.instance.decode(waterProviderData.photoData) { bitmap ->
                bitmap?.let { theDecodedBitmap ->
                    mWaterProviderWaterPhoto.setImageBitmap(theDecodedBitmap)
                }
            }
        }
    }

    private fun retrieveWaterProviderData() : WaterProvider? {
        val extras = intent.extras
        if (extras != null) {
            return extras.getParcelable(WATER_PROVIDER_INTENT_DATA_KEY)
        }
        return null
    }
}
