package com.bluewater.cleanwatermap

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.justinnguyenme.base64image.Base64Image

class WaterProviderDescriptionActivity : AppCompatActivity() {

    companion object {
        const val WATER_PROVIDER_INTENT_DATA_KEY : String = "waterProviderData"
    }

    private lateinit var mWaterProviderWaterPhoto : ImageView
    private var mWaterProvider : WaterProvider? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_water_provider_description)
        this.performLateInitView()
        this.updateWaterProviderPhotoFromIntent()
    }

    private fun performLateInitView() {
        mWaterProviderWaterPhoto = findViewById(R.id.waterProviderImage)
    }

    private fun setImageViewFromWaterProvider(aWaterProvider: WaterProvider) {
        Base64Image.instance.decode(aWaterProvider.photoData) { bitmap ->
            bitmap?.let { theDecodedBitmap ->
                mWaterProviderWaterPhoto.setImageBitmap(theDecodedBitmap)
            }
        }
    }

    private fun updateWaterProviderPhotoFromIntent() {
        val waterProviderData : WaterProvider? = this.retrieveWaterProviderData()
        if (waterProviderData != null) {
            this.setImageViewFromWaterProvider(waterProviderData)
        }
    }

    private fun retrieveWaterProviderData() : WaterProvider? {
        if (mWaterProvider != null) {
            return mWaterProvider
        }
        val extras = intent.extras
        if (extras != null) {
            mWaterProvider = extras.getParcelable(WATER_PROVIDER_INTENT_DATA_KEY)
            return mWaterProvider
        }
        return null
    }

    fun shareButtonPressed(view: View) {
        mWaterProvider?.id?.let { waterProviderID : String ->

            val rootWebsite = getString(R.string.websiteLink)
            val website  = rootWebsite + "wp/" + waterProviderID
            val sentence = getString(R.string.Refill_water_here_on_H2O_Map_App)
            val textToShare = "$sentence ${website}"
            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.type = "text/plain"
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "H2O Map")
            sharingIntent.putExtra(Intent.EXTRA_TEXT, textToShare)
            startActivity(
                Intent.createChooser(
                    sharingIntent,
                    "Share"
                )
            )
        }
    }
}
