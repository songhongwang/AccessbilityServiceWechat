package com.csizg.core

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class WxUser(var remarkName: String?, var nickName: String?) : Parcelable {
    override fun equals(other: Any?): Boolean {
        return remarkName == (other as? WxUser)?.remarkName
    }

    override fun hashCode(): Int {
        return remarkName.hashCode()
    }
}