package com.bottlerocket.test.storeresults

import android.app.AlertDialog
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.ImageView
import android.widget.TextView

import com.google.android.gms.maps.*
import com.squareup.picasso.Picasso
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.LatLng

class StoreDetailsActivity: AppCompatActivity(), StoreDataFetcher.StoreDataCallback, OnMapReadyCallback {

    private lateinit var mStoreInfo: StoreInfoModel
    private var mStoreId = StoreInfoModel.INVALID_STORE_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_store_details)

        mStoreId = intent.getStringExtra("storeID" )?.toInt() ?: mStoreId

        val storeDataFetcher = StoreDataFetcher( this, applicationContext )
        storeDataFetcher.fetch( mStoreId )
    }

    override fun onDataReceived( storeData: ArrayList<StoreInfoModel> ) {
        mStoreInfo = storeData[0]
        runOnUiThread(
                object: Runnable {
                    override fun run() {
                        val storeLogo = findViewById<ImageView>( R.id.store_icon )
                        Picasso.with(this@StoreDetailsActivity).load(mStoreInfo.storeLogoUrl.toString()).into(storeLogo)

                        val storeName = findViewById<TextView>( R.id.store_name )
                        storeName.text = mStoreInfo.name

                        val storePhone = findViewById<TextView>( R.id.store_phone )
                        storePhone.text = mStoreInfo.address.phone

                        val storeAddress = findViewById<TextView>( R.id.store_address )
                        storeAddress.text = String.format( "%s %s, %s %s",
                                mStoreInfo.address.getAddressLine( 0 ),
                                mStoreInfo.address.locality,
                                mStoreInfo.address.adminArea,
                                mStoreInfo.address.postalCode )

                        val storeCoords = findViewById<TextView>( R.id.store_coordinates )
                        storeCoords.text = String.format( "%s, %s",
                                mStoreInfo.address.latitude.toString(),
                                mStoreInfo.address.longitude )

                        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_view) as SupportMapFragment
                        mapFragment.getMapAsync(this@StoreDetailsActivity)
                    }
                }
        )
    }

    override fun onDataError() {
        val builder = AlertDialog.Builder( this )

        builder.setTitle( R.string.title_data_fetch_error )
        builder.setMessage( R.string.msg_data_fetch_error )
        builder.setNeutralButton( R.string.ok, null )

        val dialog = builder.create()
        dialog.show()
    }

    override fun onMapReady(aMap: GoogleMap?) {
        val storeLoc = LatLng( mStoreInfo.address.latitude, mStoreInfo.address.longitude )

        aMap?.addMarker( MarkerOptions().position( storeLoc )
                .title( mStoreInfo.name) )
        val defaultZoom = 16.0f
        aMap?.moveCamera( CameraUpdateFactory.newLatLngZoom( storeLoc, defaultZoom ) )
        aMap?.uiSettings?.isZoomControlsEnabled = true
        aMap?.uiSettings?.isZoomGesturesEnabled = true
    }
}