package com.example.location

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*

import io.flutter.app.FlutterActivity
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugins.GeneratedPluginRegistrant

class MainActivity: FlutterActivity() {

  private val MY_PERMISSIONS_REQUEST_LOCATION=0
  private lateinit var settingsClient: SettingsClient
  private lateinit var locationSettingsRequest: LocationSettingsRequest
  private lateinit var locationManager: LocationManager
  private lateinit var locationRequest: LocationRequest
  val GPS_REQUEST = 1001
    private lateinit var methodResult:MethodChannel.Result

  private val CHANNEL = "toast.flutter.io/toast"
  private val METHOD_LOCATION = "getLocation"

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    GeneratedPluginRegistrant.registerWith(this)


    MethodChannel(flutterView, CHANNEL).setMethodCallHandler { call, result ->
      when (call.method) {
        METHOD_LOCATION->{
            methodResult=result
          init()

          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkPermissions()) {

              turnOnGps()
            }
          } else {
            turnOnGps()
          }
        }
      }
    }

  }

  private fun init(){
    locationManager= getSystemService(Context.LOCATION_SERVICE) as LocationManager
    settingsClient= LocationServices.getSettingsClient(this)
    locationRequest= LocationRequest.create()

    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    locationRequest.setInterval(10*1000)
    locationRequest.setFastestInterval(2*1000)

    val builder=LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

    locationSettingsRequest=builder.build()

    builder.setAlwaysShow(true)
  }

  private fun turnOnGps(){
    if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
      Log.d("Main","location is provider enable")
      settingsClient.checkLocationSettings(locationSettingsRequest)
              .addOnFailureListener { e->
                val statusCode = (e as ApiException).statusCode
                when(statusCode) {
                  LocationSettingsStatusCodes.RESOLUTION_REQUIRED-> {
                    try {
                      val rae= e as ResolvableApiException
                      rae.startResolutionForResult( this, GPS_REQUEST)
                    } catch (e:Exception) {
                      Toast.makeText(this, "PendingIntent unable to execute request.", Toast.LENGTH_LONG)
                              .show()
                    }
                  }

                  LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE->{
                    val errorMessage =
                            "Location settings are inadequate, and cannot be " + "fixed here. Fix in Settings."
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG)
                            .show()
                  }
                }
              }
    }else{
      getCurrentLocation()
    }
  }

  private fun checkPermissions():Boolean{
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
      if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),MY_PERMISSIONS_REQUEST_LOCATION)
      }else{
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),MY_PERMISSIONS_REQUEST_LOCATION)
      }
      return false
    }else{
      return true
    }
  }

  private fun getCurrentLocation(){
    val mFusedLocation = LocationServices.getFusedLocationProviderClient(this)
    mFusedLocation.lastLocation.addOnSuccessListener {
      Toast.makeText(this,"lat: ${it.latitude}, lng: ${it.longitude}", Toast.LENGTH_SHORT).show()
//        val loc=Location()
        methodResult.success("""{"lat": "${it.latitude}", "long": "${it.longitude}"}""")
    }
  }

  override fun onRequestPermissionsResult(
          requestCode: Int,
          permissions: Array<out String>,
          grantResults: IntArray
  ) {
    when(requestCode){
      MY_PERMISSIONS_REQUEST_LOCATION->{
        if (grantResults.isNotEmpty()&& grantResults[0]== PackageManager.PERMISSION_GRANTED){
          getCurrentLocation()
        }else{
          Toast.makeText(this,"permission denied", Toast.LENGTH_SHORT).show()
        }
      }

    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (resultCode == Activity.RESULT_OK) {
      if (requestCode == GPS_REQUEST) {
        getCurrentLocation()// flag maintain before get location
      }
    }
  }
}
