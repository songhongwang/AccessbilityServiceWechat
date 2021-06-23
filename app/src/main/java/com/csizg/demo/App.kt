package com.csizg.demo

import android.app.Application
import android.content.IntentFilter
import com.csizg.core.AsCore
import com.csizg.core.WxUser
import java.util.LinkedHashSet
import kotlin.collections.HashMap

class App : Application() {
    var users: ArrayList<WxUser> = ArrayList<WxUser>()

    companion object {
        lateinit var mApp: App

    }

    init {
        mApp = this
    }

    override fun onCreate() {
        super.onCreate()

        val filter = IntentFilter()
        filter.addAction(AsCore.BROADCAST_ACTION)
        val wxContactsReceiver = MyReceiver()
        registerReceiver(wxContactsReceiver, filter)
    }
}