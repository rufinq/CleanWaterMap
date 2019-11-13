package com.example.cleanwatermap

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
// import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
// import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.util.Log
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import java.security.AccessController.getContext

import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import androidx.core.app.ActivityCompat

import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import com.tbruyelle.rxpermissions2.RxPermissions

import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T




class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        const val TAG : String = "MapsActivity"
    }

    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
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

        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
    }

    fun addButtonPressed(view: android.view.View) {
        // do stuff
        takeWaterDispensaryPhoto()
    }

     private fun askForRunTimeCameraPermission() {
//         RxPermissions(this)
//             .request(Manifest.permission.CAMERA) // ask single or multiple permission once
//             .subscribe({ granted ->
//                 if (granted) {
//                    Log.v(TAG, "Camera permission alloweed")
//                 } else {
//                     Log.e("Camera permission denied")
//                 }
//             })
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
         val pm = getApplicationContext().getPackageManager()
        return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    private fun takeWaterDispensaryPhoto() {
        askForRunTimeCameraPermission()
        if (doesTheUserGrantedCameraPermission()) {

        }
        else
        {
            Log.e(TAG, "ERROR : Camera feature is deactivated at runtime")
        }
    }
}
