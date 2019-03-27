package com.bottlerocket.test.storeresults

import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import com.squareup.picasso.Picasso

class StoreInfoAdapter(
    private val mStoreInfoSet: ArrayList<StoreInfoModel> )
    : RecyclerView.Adapter<StoreInfoAdapter.StoreInfoViewHolder>() {

    class StoreInfoViewHolder( private val mView: View ) : RecyclerView.ViewHolder( mView ) {

        private lateinit var mStoreInfo: StoreInfoModel

        private val storeIconImageView: ImageView
        private val storeNameTextView: TextView
        private val storePhoneNumTextView: TextView

        init {
            storeIconImageView = mView.findViewById( R.id.store_icon )
            storeNameTextView = mView.findViewById( R.id.store_name )
            storePhoneNumTextView = mView.findViewById( R.id.store_phone )
        }

        fun bindStoreInfo( aStoreInfo: StoreInfoModel ) {
            mStoreInfo = aStoreInfo

            Picasso.with( mView.context ).load( mStoreInfo.storeLogoUrl.toString() ).into( storeIconImageView )
            storeNameTextView.text = mStoreInfo.name
            storePhoneNumTextView.text = mStoreInfo.address.phone

            mView.setOnClickListener {
                val context = mView.context
                val storeViewIntent = Intent( context, StoreDetailsActivity::class.java )
                storeViewIntent.putExtra( "storeID", mStoreInfo.storeId )
                context.startActivity( storeViewIntent )
            }
        }
    }

    override fun onCreateViewHolder(
        aViewGroup: ViewGroup,
        viewType: Int )
        : StoreInfoViewHolder {
        val storeInfoView = LayoutInflater.from( aViewGroup.context )
                .inflate( R.layout.store_info_row, aViewGroup, false )

        return StoreInfoViewHolder( storeInfoView )
    }

    override fun onBindViewHolder( aViewHolder: StoreInfoViewHolder, position: Int ) {
        aViewHolder.bindStoreInfo( mStoreInfoSet[position] )
    }

    override fun getItemCount() = mStoreInfoSet.size

}