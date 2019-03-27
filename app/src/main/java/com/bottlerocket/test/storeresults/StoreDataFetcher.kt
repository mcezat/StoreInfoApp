package com.bottlerocket.test.storeresults

import android.content.Context
import android.location.Address
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri.Builder
import android.os.Handler
import android.os.HandlerThread

import okhttp3.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import java.net.URL
import java.util.*
import java.io.*
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

// NOTE: If I had more time, I would definitely refactor this (:
class StoreDataFetcher( private val mResponseListener: StoreDataCallback,
                        private val mContext: Context) {

    interface StoreDataCallback {
        fun onDataReceived( storeData: ArrayList<StoreInfoModel> )
        fun onDataError()
    }

    private var mStoreInfo : ArrayList<StoreInfoModel>? = null
    private var mLastUpdatedDate : LocalDateTime? = null
    private var mValidDataMs : Long = 1000 * 60 * 30
    private var mHandler: Handler
    private var mHandlerThread: HandlerThread

    init {
        mHandlerThread = HandlerThread( "FetchThread" )
        mHandlerThread.start()
        mHandler = Handler( mHandlerThread.looper )
    }

    fun fetch( aStoreID: Int = StoreInfoModel.INVALID_STORE_ID ) {
        mHandler.post(
            object: Runnable {
                override fun run() {
                    checkAndloadCache()

                    var storeData = mStoreInfo
                    if( storeData == null ) {
                        fetchDataFromNetwork(aStoreID)
                    } else {
                        if( aStoreID != StoreInfoModel.INVALID_STORE_ID ) {
                            storeData = getStoreById( storeData, aStoreID )
                        }
                        mResponseListener.onDataReceived( storeData )
                    }
                }
            }
        )
    }

    private fun getStoreById( aStoreInfoList: ArrayList<StoreInfoModel>,
                              aStoreID: Int ) : ArrayList<StoreInfoModel> {
        val retList = ArrayList<StoreInfoModel>()
        retList.add( aStoreInfoList.first { store -> store.storeId.toInt() == aStoreID } )
        return retList
    }

    private fun checkAndloadCache() {
        if( mStoreInfo == null) {
            val cacheDir = mContext.externalCacheDir

            val storeDataFile = File( cacheDir?.absolutePath + STORE_INFO_FILE_NAME )
            if( storeDataFile.isFile ) {
                if( isDataExpired() ) {
                    storeDataFile.delete()
                } else {
                    val reader = BufferedReader( FileReader( storeDataFile ) )
                    val storeInfoJson = JSONArray( reader.readText() )
                    mStoreInfo = parseStoreInfoJson(storeInfoJson)
                }
            }
        }
    }

    private fun isDataExpired() : Boolean {
        var isExpired = true
        if( mLastUpdatedDate == null ) {
            val prefs = mContext.getSharedPreferences( STORE_INFO_SHARED_PREF, 0 )
            val serializedDate = prefs.getString( STORE_INFO_LAST_UPDATED_KEY, "" )
            if( serializedDate != "" ) {
                mLastUpdatedDate = LocalDateTime.parse( serializedDate )
            }
        }

        if( mLastUpdatedDate != null ) {
            val now = LocalDateTime.now()
            isExpired = (now.minus(mValidDataMs, ChronoUnit.MILLIS).isAfter( mLastUpdatedDate ))
        }
        return isExpired
    }

    private fun fetchDataFromNetwork( aStoreID: Int = StoreInfoModel.INVALID_STORE_ID ) {
        if( hasNetworkConnection() ) {
            val urlRequest = Builder().scheme(URL_SCHEME)
                .authority(URL_AUTHORITY)
                .appendPath(URL_PATH)
                .appendPath(STORE_INFO_FILE_NAME)
                .build().toString()

            val request = Request.Builder().url(urlRequest).build()

            val httpClient = OkHttpClient()
            httpClient.newCall(request).enqueue( object: Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        val theJSONResponse = JSONObject(response.body()!!.string())

                        val storeDataJsonArray = JSONArray(theJSONResponse.getString("stores"))
                        val lastUpdated = LocalDateTime.now()
                        cacheStoreData( storeDataJsonArray, lastUpdated )

                        var storeInfoList: ArrayList<StoreInfoModel> = ArrayList()

                        for( i in 0 until storeDataJsonArray.length() ) {
                            val storeInfoJSON = storeDataJsonArray.getJSONObject(i)

                            val theStoreID = storeInfoJSON.getString( "storeID" )

                            val theName = storeInfoJSON.getString( "name" )

                            val theAddress: Address = Address(Locale.US)
                            theAddress.longitude = storeInfoJSON.getDouble( "longitude" )
                            theAddress.latitude = storeInfoJSON.getDouble( "latitude" )
                            theAddress.postalCode = storeInfoJSON.getString( "zipcode" )
                            theAddress.locality = storeInfoJSON.getString( "city" )
                            theAddress.adminArea = storeInfoJSON.getString( "state" )
                            theAddress.phone = storeInfoJSON.getString( "phone" )
                            theAddress.setAddressLine( 0, storeInfoJSON.getString( "address" ) )

                            val theIconURL = URL( storeInfoJSON.getString( "storeLogoURL" ) )

                            storeInfoList.add( StoreInfoModel( theStoreID, theName, theAddress, theIconURL ) )
                        }

                        mStoreInfo = storeInfoList
                        if( aStoreID != StoreInfoModel.INVALID_STORE_ID ) {
                            storeInfoList = getStoreById( storeInfoList, aStoreID )
                        }
                        mResponseListener.onDataReceived( storeInfoList )
                    } catch( e: JSONException ) {
                        e.printStackTrace()
                    }
                }
            })
        } else {
            mResponseListener.onDataError()
        }
    }

    private fun hasNetworkConnection(): Boolean {
        val connectivityManager = mContext.getSystemService( Context.CONNECTIVITY_SERVICE ) as ConnectivityManager
        val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
        return activeNetwork?.isConnectedOrConnecting ?: false
    }

    private fun cacheStoreData( aJsonArray : JSONArray,
                        aLastUpdated : LocalDateTime ) {
        val cacheDir = mContext.externalCacheDir

        try {
            val storeDataFile = File( cacheDir?.absolutePath + STORE_INFO_FILE_NAME )
            val writer = BufferedWriter( FileWriter( storeDataFile ) )
            writer.write( aJsonArray.toString() )
            writer.close()

            val prefs = mContext.getSharedPreferences( STORE_INFO_SHARED_PREF, 0 )
            prefs.edit().putString( STORE_INFO_LAST_UPDATED_KEY, aLastUpdated.toString() ).apply()
        } catch( e: Exception ) {
            e.printStackTrace()
        }
    }

    private fun parseStoreInfoJson( aJsonArray : JSONArray ) : ArrayList<StoreInfoModel>? {
        var storeInfoList: ArrayList<StoreInfoModel> = ArrayList()

        for( i in 0 until aJsonArray.length() ) {
            val storeInfoJSON = aJsonArray.getJSONObject( i )

            val theStoreID = storeInfoJSON.getString( "storeID" )

            val theName = storeInfoJSON.getString( "name" )

            val theAddress: Address = Address( Locale.US )
            theAddress.longitude = storeInfoJSON.getDouble( "longitude" )
            theAddress.latitude = storeInfoJSON.getDouble( "latitude" )
            theAddress.postalCode = storeInfoJSON.getString( "zipcode" )
            theAddress.locality = storeInfoJSON.getString( "city" )
            theAddress.adminArea = storeInfoJSON.getString( "state" )
            theAddress.phone = storeInfoJSON.getString( "phone" )
            theAddress.setAddressLine( 0, storeInfoJSON.getString( "address" ) )

            val theIconURL = URL( storeInfoJSON.getString( "storeLogoURL" ) )

            storeInfoList.add( StoreInfoModel( theStoreID, theName, theAddress, theIconURL ) )
        }
        return storeInfoList
    }

    companion object {
        private const val URL_SCHEME = "http"
        private const val URL_AUTHORITY = "sandbox.bottlerocketapps.com"
        private const val URL_PATH = "BR_Android_CodingExam_2015_Server"
        private const val STORE_INFO_FILE_NAME = "stores.json"
        private const val STORE_INFO_SHARED_PREF = "com.bottlerocket.test.storeresults.prefs"
        private const val STORE_INFO_LAST_UPDATED_KEY = "last_updated"
    }
}