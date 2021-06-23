package com.csizg.demo

import android.content.Context
import android.content.Intent
import android.util.Log
import com.csizg.core.WxContactsReceiver
import com.csizg.core.WxUser

class MyReceiver : WxContactsReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        val list = intent?.extras?.getParcelableArrayList<WxUser>("wxContactsList")
        Log.d("song___", "broadcast size = "+list?.size)

        list?.let {
            App.mApp.users.clear()
            App.mApp.users.addAll(list.take(5))

        }

    }
}