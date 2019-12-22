package com.example.cleanwatermap

// import androidx.core.app.ComponentActivity.ExtraData
// import androidx.core.app.ComponentActivity.ExtraData

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.jakewharton.threetenabp.AndroidThreeTen
import com.tbruyelle.rxpermissions2.RxPermissions
import es.dmoral.toasty.Toasty
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import retrofit2.Call
import retrofit2.Response
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.util.concurrent.TimeUnit


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    companion object {
        const val TAG : String = "MapsActivity"
        const val MarkerHashMapDefaultInitSize : Int = 100
        const val TIME_PERIOD_BETWEEN_NETWORK_RETRY : Long = 10000 // in milliseconds
    }

    private lateinit var mMap: GoogleMap
    private val REQUEST_IMAGE_CAPTURE = 1

    private lateinit var mMarkersHashMap : HashMap<Marker, WaterProvider>

    private lateinit var mWaterProviderDownloader :  Disposable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidThreeTen.init(this)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        this.initTimber()
        this.removeTitleBar()
        mMarkersHashMap = HashMap(MarkerHashMapDefaultInitSize)
    }

    private fun initTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }
    }

    private fun removeTitleBar() {
        if (supportActionBar != null)
            supportActionBar?.hide()
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val planter = LatLng(18.806909, 98.964652)
        //mMap.addMarker(MarkerOptions().position(planter).title("Planter 's Space"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(planter))
        mMap.animateCamera(CameraUpdateFactory.zoomTo( 16.0f ))
        // TODO improve this code bellow
        this.requestRuntimeLocationPermission()
        this.getAllWaterProviderOnMapAndRetryOnFailure()
        mMap.setOnMarkerClickListener(this)
        mMap.setOnInfoWindowClickListener {marker: Marker? ->
            if (marker != null) {
                switchToWaterProviderDescriptionActivityWithAPICallFromMarker(marker)
            }
        }
    }

    fun requestRuntimeLocationPermission() {
        RxPermissions(this).request(Manifest.permission.ACCESS_FINE_LOCATION).subscribe(
            { granted ->
                if (granted) {
                    this.showUserUserPositionAndLocationButton()
                }
                else
                {
                    Toasty.error(getApplicationContext(), "GPS permission refused")
                }
            })
    }

    fun showUserUserPositionAndLocationButton() {
        mMap.setMyLocationEnabled(true)
    }

    fun getBitmapDescriptorFromTDSValue(aTDSValue : Int) :  Float {
        var result = when (aTDSValue) {
            0 -> BitmapDescriptorFactory.HUE_ROSE
            in 1..30 -> BitmapDescriptorFactory.HUE_GREEN
            in 31..50 -> BitmapDescriptorFactory.HUE_YELLOW
            in 51..75 -> BitmapDescriptorFactory.HUE_ORANGE
            else ->  BitmapDescriptorFactory.HUE_RED
        }
        return result
    }

    private fun updateGoogleMapAndMarkerHashMapFromWaterProviderList(waterProviders : List<WaterProvider>) {
        for (aWaterProvider : WaterProvider in waterProviders)  {
            val TDSValue : Int = aWaterProvider.tdsMeasurements.last().tdsValue
            val title : String = "TDS Value: $TDSValue"
            val position = aWaterProvider.waterProviderLocation.convertToLatLng()
            var aMarkerOption = MarkerOptions().position(position).title(title)
            val floatColor = this.getBitmapDescriptorFromTDSValue(TDSValue)
            aMarkerOption = aMarkerOption.icon(BitmapDescriptorFactory.defaultMarker(floatColor))
            val addedMarker = mMap.addMarker(aMarkerOption)
            mMarkersHashMap.put(addedMarker, aWaterProvider)
        }
    }

    private fun cancelWaterProviderDownloadingRetry() {
        mWaterProviderDownloader.dispose()
    }

    private fun getAllWaterProviderOnMapAndRetryOnFailure() {
        mWaterProviderDownloader = Observable.interval(0, TIME_PERIOD_BETWEEN_NETWORK_RETRY, TimeUnit.MILLISECONDS)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe {
            var APICall : Call<List<WaterProvider>> = CleanWaterMapServerAPISingleton.API().waterProviders
            APICall.enqueue {
                onResponse = {response: Response<List<WaterProvider>> ->
                    if (response.isSuccessful() && response.body() != null) {
                        val waterProviders : List<WaterProvider>? = response.body()
                        if (waterProviders != null) {
                            updateGoogleMapAndMarkerHashMapFromWaterProviderList(waterProviders)
                        }
                    }
                    cancelWaterProviderDownloadingRetry()
                }
                onFailure = {
                    Toasty.warning(applicationContext, "App Unable to connect to server").show()
                }
            }
        }
    }

    fun addButtonPressed(view: android.view.View) {
        takeWaterDispensaryPhoto()
    }

     private fun askForRunTimeCameraPermission() {
         RxPermissions(this)
             .request(Manifest.permission.CAMERA) // ask single or multiple permission once
             .subscribe { granted ->
                 if (granted!!) {
                     Log.v(TAG, "Camera permission allowed")
                 } else {
                     Log.e(TAG, "Camera permission denied")
                 }
             }
     }

    private fun doesTheUserGrantedCameraPermission() : Boolean {
        val pm = applicationContext.packageManager
        return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    private fun takeWaterDispensaryPhoto() {
        askForRunTimeCameraPermission()
        if (doesTheUserGrantedCameraPermission()) {
            dispatchTakePictureIntent()
        }
        else
        {
            Log.e(TAG, "ERROR : Camera feature is deactivated at runtime")
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)

            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK && data != null) {
            val extras: Bundle? = data.getExtras()
            if (extras != null) {
                val photo: Bitmap? = extras.get("data") as Bitmap
                if (photo != null) {
                    switchToAddingWaterRefillStationActivityWithPhotoData(photo)
                }
            }
        }
    }

    private fun switchToAddingWaterRefillStationActivityWithPhotoData(photoData: Bitmap) {
        val intent = Intent(this, AddingWaterRefillStationActivity::class.java)
        intent.putExtra("data", photoData)
        startActivity(intent)
    }

    private fun switchToWaterProviderDescriptionActivityWithAPICallFromMarker(aMarker: Marker) {
        val aWaterProvider : WaterProvider? = mMarkersHashMap[aMarker]
        if (aWaterProvider != null) {
            var APICall : Call<WaterProvider> = CleanWaterMapServerAPISingleton.API().getOneWaterProvider(aWaterProvider.id)
            APICall.enqueue {
                onResponse = {response ->
                    if (response.isSuccessful()) {
                        val theWaterProviderClicked : WaterProvider? = response.body()
                        theWaterProviderClicked?.let {
                            val intent = Intent(getBaseContext(), WaterProviderDescriptionActivity::class.java)
                            intent.putExtra(WaterProviderDescriptionActivity.WATER_PROVIDER_INTENT_DATA_KEY, theWaterProviderClicked)
                            startActivity(intent)
                        }
                    }
                    onFailure = {
                        Toasty.warning(applicationContext ,"Unable to connect to server").show()
                    }
                }
            }
        }
    }

    override fun onMarkerClick(aMarker : Marker) : Boolean {
        return false
    }
}
