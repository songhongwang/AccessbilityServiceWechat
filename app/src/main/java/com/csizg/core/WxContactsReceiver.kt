package com.csizg.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

open class WxContactsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val list = intent?.extras?.getParcelableArrayList<WxUser>("wxContactsList")
        Log.d("song___", "broadcast size = "+list?.size)
        // TODO: 2021/6/22 过滤数据或者预处理

    }


}