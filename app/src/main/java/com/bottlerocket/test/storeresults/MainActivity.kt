package com.bottlerocket.test.storeresults

import android.app.AlertDialog
import android.content.DialogInterface
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView

class MainActivity : AppCompatActivity(), StoreDataFetcher.StoreDataCallback, DialogInterface.OnClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var mAdapter: StoreInfoAdapter
    private var mStoreInfoList: ArrayList<StoreInfoModel> = ArrayList()

    override fun onCreate( savedInstanceState: Bundle? ) {
        super.onCreate( savedInstanceState )
        setContentView( R.layout.activity_main )

        recyclerView = findViewById( R.id.store_info_list )
        linearLayoutManager = LinearLayoutManager( this, LinearLayoutManager.VERTICAL, false )
        recyclerView.layoutManager = linearLayoutManager

        mAdapter = StoreInfoAdapter(mStoreInfoList)
        recyclerView.adapter = mAdapter

        val storeDataFetcher = StoreDataFetcher( this, applicationContext )
        storeDataFetcher.fetch()
    }

    override fun onDataReceived( storeData: ArrayList<StoreInfoModel> ) {
        runOnUiThread(
            object: Runnable {
                override fun run() {
                    mStoreInfoList.addAll( storeData )
                    mAdapter.notifyDataSetChanged()
                }
            }
        )
    }

    override fun onClick( dialog: DialogInterface?, which: Int ) {
        finish()
    }

    override fun onDataError() {
        val builder = AlertDialog.Builder( this )

        builder.setTitle( R.string.title_data_fetch_error )
        builder.setMessage( R.string.msg_data_fetch_error )
        builder.setNeutralButton( R.string.ok, this )

        val dialog = builder.create()
        dialog.show()
    }
}
