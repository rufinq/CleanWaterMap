package com.bluewater.cleanwatermap

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View

import androidx.appcompat.app.AppCompatActivity
import com.bluewater.cleanwatermap.TDSMeasurement.SAFE_TDS_VALUE_LIMIT
import com.bluewater.cleanwatermap.TDSMeasurement.UNTESTED_WATER_VALUE
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.jakewharton.threetenabp.AndroidThreeTen
import com.justinnguyenme.base64image.Base64Image
import com.tbruyelle.rxpermissions2.RxPermissions
import es.dmoral.toasty.Toasty
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import retrofit2.Response
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.util.concurrent.TimeUnit


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    companion object {
        const val MarkerHashMapDefaultInitSize: Int = 100
        const val TIME_PERIOD_BETWEEN_NETWORK_RETRY: Long = 10000 // in milliseconds
        const val REQUEST_IMAGE_CAPTURE = 1
        const val FILTER_ACTIVITY_REQUEST_CODE = 0
        const val DISTANCE_THRESHOLD_BETWEEN_2_WATER_PROVIDERS : Float = 10f // in meters
    }

    private lateinit var mMap: GoogleMap

    private lateinit var mMarkersHashMap: HashMap<Marker, WaterProvider>
    private var mSavedWaterProviders: List<WaterProvider>? = null

    private lateinit var mWaterProviderDownloader: Disposable

    private val mCompositeDisposable = CompositeDisposable()

    private lateinit var mFusedLocationClient: FusedLocationProviderClient

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
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        handleAppLinkIntent()
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
        mMap.moveCamera(CameraUpdateFactory.newLatLng(planter))
        mMap.animateCamera(CameraUpdateFactory.zoomTo(16.0f))
        // TODO improve this code bellow
        this.requestRuntimeLocationPermission(grantedCallBack = {
            this.showUserUserPositionAndLocationButton()
        })
        this.getAllWaterProviderOnMapAndRetryOnFailure()
        mMap.setOnMarkerClickListener(this)
        mMap.setOnInfoWindowClickListener { marker: Marker? ->
            if (marker != null) {
                switchToWaterProviderDescriptionActivityWithAPICallFromMarker(marker)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAppLinkIntent()
    }

    private fun handleAppLinkIntent() {
        val appLinkIntent : Intent = intent
        val appLinkData : Uri? = appLinkIntent.data
        if (appLinkData != null) {
            val waterProviderID : String? = appLinkData.lastPathSegment
            CleanWaterMapServerAPISingleton.API().getOneWaterProvider(waterProviderID).enqueue {
                onResponse = {waterProviderResponse ->
                    if (waterProviderResponse.isSuccessful)  {
                        waterProviderResponse.body()?.let {waterProvider ->
                            val intent = Intent(baseContext, WaterProviderDescriptionActivity::class.java)
                            intent.putExtra(WaterProviderDescriptionActivity.WATER_PROVIDER_INTENT_DATA_KEY, waterProvider)
                            startActivity(intent)
                        }
                    }
                    else {
                        // TODO internationalization here
                        Toasty.warning(applicationContext, "Incorrect Link")
                            .show()
                    }
                }
                onFailure = {
                    // TODO internationalization here
                    Toasty.warning(applicationContext, "App unable to connect to server")
                        .show()
                }
            }
        }
    }

    private fun requestRuntimeLocationPermission(grantedCallBack : ()->Unit) {
        if (!this.isFinishing && !this.isDestroyed) {
            val disposable = RxPermissions(this).request(Manifest.permission.ACCESS_FINE_LOCATION).subscribe()
            { granted ->
                if (granted) {
                    grantedCallBack()
                } else {
                    Toasty.error(applicationContext, "GPS permission refused")
                }
            }
            mCompositeDisposable.add(disposable)
        }
    }

    private fun showUserUserPositionAndLocationButton() {
        mMap.isMyLocationEnabled = true
    }

    private fun getBitmapDescriptorFromTDSValue(aTDSValue: Int): Float {
        return when (aTDSValue) {
            0 -> BitmapDescriptorFactory.HUE_ROSE
            in 1..SAFE_TDS_VALUE_LIMIT -> BitmapDescriptorFactory.HUE_GREEN
            in 31..50 -> BitmapDescriptorFactory.HUE_YELLOW
            in 51..75 -> BitmapDescriptorFactory.HUE_ORANGE
            else -> BitmapDescriptorFactory.HUE_RED
        }
    }

    private fun removeMarkersOnMap() {
        mMap.clear()
    }

    private fun createTitleForMarkerOptionFromTDSValue(tdsValue : Int) : String {
        // TODO to internationalize here
        if (tdsValue == UNTESTED_WATER_VALUE) {
            return "Untested Water"
        }
        return "TDS Value: $tdsValue"
    }

    private fun createMarkerOptionsFromAWaterProvider(aWaterProvider: WaterProvider): MarkerOptions {
        val theTDSValue: Int = aWaterProvider.lastTDSMeasurementValue()
        val title = this.createTitleForMarkerOptionFromTDSValue(theTDSValue)
        val position = aWaterProvider.waterProviderLocation.convertToLatLng()
        val aMarkerOption = MarkerOptions().position(position).title(title)
        val floatColor = this.getBitmapDescriptorFromTDSValue(theTDSValue)
        return aMarkerOption.icon(BitmapDescriptorFactory.defaultMarker(floatColor))
    }

    private fun addMarkerToMapAndHashMap(
        markerOptions: MarkerOptions,
        aWaterProvider: WaterProvider
    ) {
        val addedMarker = mMap.addMarker(markerOptions)
        mMarkersHashMap[addedMarker] = aWaterProvider
    }

    private fun addMarkerToMapAndHashMapWithDefaultMarkerOptions(aWaterProvider: WaterProvider) {
        val aMarkerOption: MarkerOptions = this.createMarkerOptionsFromAWaterProvider(aWaterProvider)
        this.addMarkerToMapAndHashMap(aMarkerOption, aWaterProvider)
    }

    private fun addAllMarkersToMapAndHashMapWithDefaultMarkerOptions(waterProviders : List<WaterProvider>) {
        for (aWaterProvider: WaterProvider in waterProviders) {
            this.addMarkerToMapAndHashMapWithDefaultMarkerOptions(aWaterProvider)
        }
    }

    private fun updateGoogleMapAndMarkerHashMapFromWaterProviderListAndFilter(waterProviders: List<WaterProvider>) {

        this.removeMarkersOnMap()
        mMarkersHashMap.clear()
        if (Settings.userSpecifiedFilter) {
            mFusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                // Got last known location. In some rare situations this can be null.
                if (location != null) {
                    for (aWaterProvider: WaterProvider in waterProviders) {
                        if (Settings.matchFilterWithLocationAndWaterProvider(location, aWaterProvider)) {
                            this.addMarkerToMapAndHashMapWithDefaultMarkerOptions(aWaterProvider)
                        }
                    }
                } else {
                    Timber.e("location is null on 'updateGoogleMapAndMarkerHashMapFromWaterProviderListAndFilter")
                    this.addAllMarkersToMapAndHashMapWithDefaultMarkerOptions(waterProviders)
                }
            }
        } else {
            this.addAllMarkersToMapAndHashMapWithDefaultMarkerOptions(waterProviders)
        }
    }

    private fun updateGoogleMapAndMarkerHashMapFromWaterProviderList(waterProviders: List<WaterProvider>) {
        this.updateGoogleMapAndMarkerHashMapFromWaterProviderListAndFilter(waterProviders)
    }

    private fun cancelWaterProviderDownloadingRetry() {
        mWaterProviderDownloader.dispose()
    }

    private fun saveWaterProviders(waterProviders: List<WaterProvider>?) {
        check(waterProviders != null)
        mSavedWaterProviders = waterProviders
    }

    private fun showWarningToastyAppUnableToConnecToServer() {
        // TODO internationalization here
        Toasty.warning(applicationContext, "App Unable to connect to server")
            .show()
    }

    private fun getAllWaterProviderOnMapAndRetryOnFailure() {
        mWaterProviderDownloader =
            Observable.interval(0, TIME_PERIOD_BETWEEN_NETWORK_RETRY, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    CleanWaterMapServerAPISingleton.API().waterProviders.enqueue {
                        onResponse = { response: Response<List<WaterProvider>> ->
                            if (response.isSuccessful && response.body() != null) {
                                val waterProviders: List<WaterProvider>? = response.body()
                                saveWaterProviders(waterProviders)
                                if (waterProviders != null) {
                                    updateGoogleMapAndMarkerHashMapFromWaterProviderList(waterProviders)
                                }
                            }
                            cancelWaterProviderDownloadingRetry()
                        }
                        onFailure = {
                            showWarningToastyAppUnableToConnecToServer()
                        }
                    }
                }
    }

    private fun updateLastLocation() {
        mFusedLocationClient.lastLocation

    }

    private fun closestWaterProviderFromLastLocation(
        onLocationSuccess : (closestWaterProviderWithoutPhoto : WaterProvider?, distanceFromUser : Float)->Unit
    ) {
        var waterProviderToReturn : WaterProvider? = null
        var minimumDistance = Float.MAX_VALUE
        val savedWaterProviders = mSavedWaterProviders
        updateLastLocation()
        if (savedWaterProviders != null && savedWaterProviders.isNotEmpty()) {
             mFusedLocationClient.lastLocation.addOnSuccessListener {lastLocation: Location? ->
                 if (lastLocation != null) {
                     for (waterProvider in savedWaterProviders) {
                         val distanceToWaterProvider : Float = waterProvider.distanceTo(lastLocation)
                         if (distanceToWaterProvider < minimumDistance) {
                             waterProviderToReturn = waterProvider
                             minimumDistance = distanceToWaterProvider
                         }
                     }
                     onLocationSuccess(waterProviderToReturn, minimumDistance)
                 }
             }
         }
    }

    private fun switchToDuplicateWaterProviderActivity(newPhoto: Bitmap, closestWaterProviderWithoutPhoto: WaterProvider) {
        val intent = Intent(this, DuplicateWaterProviderActivity::class.java)
        CleanWaterMapServerAPISingleton.API().getOneWaterProvider(closestWaterProviderWithoutPhoto.id).enqueue {
            onResponse = {response ->
                if (response.isSuccessful) {
                    val closestWaterProviderWithPhoto = response.body()
                    if (closestWaterProviderWithPhoto != null) {
                        bitmapPhotoFromWaterProvider(closestWaterProviderWithPhoto, bitmapCallBack = {closestWaterProviderBitMap ->
                            intent.putExtra(DuplicateWaterProviderActivity.NEW_PHOTO_KEY_INTENT_DATA_KEY, newPhoto)
                            intent.putExtra(DuplicateWaterProviderActivity.DB_PHOTO_KEY_INTENT_DATA_KEY, closestWaterProviderBitMap)
                            startActivity(intent)
                        })
                    }
                }
                onFailure = {
                    showWarningToastyAppUnableToConnecToServer()
                }
            }

        }
    }

    private fun bitmapPhotoFromWaterProvider(aWaterProvider: WaterProvider, bitmapCallBack : (Bitmap)->Unit) {
        Base64Image.instance.decode(aWaterProvider.photoData) { bitmap ->
            if (bitmap != null ) {
                bitmapCallBack(bitmap)
            }
            else {
                Timber.e("bitmap is null on bitmapPhotoFromWaterProvider method")
            }
        }
    }

    fun addButtonPressed(@Suppress("UNUSED_PARAMETER") view: View) {
        /*
            updateLastLocation()
            The goal is to update the location when the user is taking the water provider picture.
         */
        updateLastLocation()
        takeWaterDispensaryPhoto()
    }

    private fun askForRunTimeCameraPermission() {
        val disposable = RxPermissions(this)
            .request(Manifest.permission.CAMERA) // ask single or multiple permission once
            .subscribe { granted ->
                if (granted!!) {
                    Timber.v("Camera permission allowed")
                } else {
                    Timber.e("Camera permission denied")
                }
            }
        mCompositeDisposable.add(disposable)
    }

    private fun doesTheUserGrantedCameraPermission(): Boolean {
        val pm = applicationContext.packageManager
        return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    private fun takeWaterDispensaryPhoto() {
        askForRunTimeCameraPermission()
        if (doesTheUserGrantedCameraPermission()) {
            dispatchTakePictureIntent()
        } else {
            Timber.e("ERROR : Camera feature is deactivated at runtime")
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
        val extras: Bundle? = data?.extras
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK && data != null) {
            if (extras != null) {
                val photo: Bitmap? = extras.get(AddingWaterRefillStationActivity.NEW_PHOTO_KEY_INTENT_DATA_KEY) as Bitmap
                if (photo != null) {
                    this.closestWaterProviderFromLastLocation {closestWaterProviderWithoutPhoto, distanceFromUser : Float ->
                        val isClosestWaterProviderBellowDistanceThreshold = distanceFromUser < DISTANCE_THRESHOLD_BETWEEN_2_WATER_PROVIDERS
                        if (isClosestWaterProviderBellowDistanceThreshold && closestWaterProviderWithoutPhoto != null) {
                            this.switchToDuplicateWaterProviderActivity(photo, closestWaterProviderWithoutPhoto)
                        }
                        else
                            switchToAddingWaterRefillStationActivityWithPhotoData(photo)
                    }
                }
            }
        } else if (requestCode == FILTER_ACTIVITY_REQUEST_CODE) {
            this.filterWaterProviders()
        }
    }

    private fun filterWaterProviders() {
        mSavedWaterProviders?.also { waterProviders ->
            this.updateGoogleMapAndMarkerHashMapFromWaterProviderListAndFilter(waterProviders)
        }
    }

    private fun switchToAddingWaterRefillStationActivityWithPhotoData(photoData: Bitmap) {
        val intent = Intent(this, AddingWaterRefillStationActivity::class.java)
        intent.putExtra(AddingWaterRefillStationActivity.NEW_PHOTO_KEY_INTENT_DATA_KEY, photoData)
        startActivity(intent)
    }

    private fun switchToWaterProviderDescriptionActivityWithAPICallFromWaterProviderWithoutPhotoLoaded(aWaterProvider: WaterProvider) {
        CleanWaterMapServerAPISingleton.API().getOneWaterProvider(aWaterProvider.id).enqueue {
            onResponse = { response ->
                if (response.isSuccessful) {
                    val theWaterProviderClicked: WaterProvider? = response.body()
                    theWaterProviderClicked?.let {
                        val intent =
                            Intent(baseContext, WaterProviderDescriptionActivity::class.java)
                        intent.putExtra(
                            WaterProviderDescriptionActivity.WATER_PROVIDER_INTENT_DATA_KEY,
                            theWaterProviderClicked
                        )
                        startActivity(intent)
                    }
                }
                onFailure = {
                    Toasty.warning(applicationContext, "Unable to connect to server").show()
                }
            }
        }
    }

    private fun switchToWaterProviderDescriptionActivityWithAPICallFromMarker(aMarker: Marker) {
        val aWaterProvider: WaterProvider? = mMarkersHashMap[aMarker]
        if (aWaterProvider != null) {
            this.switchToWaterProviderDescriptionActivityWithAPICallFromWaterProviderWithoutPhotoLoaded(aWaterProvider)
        }
    }

    fun filterButtonPressed(@Suppress("UNUSED_PARAMETER") view: View) {
        val intent = Intent(this, FilterActivity::class.java)
        startActivityForResult(intent, FILTER_ACTIVITY_REQUEST_CODE)
    }

    override fun onMarkerClick(aMarker: Marker): Boolean {
        return false
    }

    override fun onDestroy() {
        mCompositeDisposable.dispose()
        super.onDestroy()
    }
}
