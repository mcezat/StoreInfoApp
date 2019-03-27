package com.bottlerocket.test.storeresults

import android.location.Address
import java.net.URL

data class StoreInfoModel (
    val storeId: String,
    val name: String,
    val address: Address,
    val storeLogoUrl: URL
) {
    companion object {
        const val INVALID_STORE_ID = -1
    }
}