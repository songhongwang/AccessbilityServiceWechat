package com.csizg.demo

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils.SimpleStringSplitter
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.csizg.core.AsCore
import com.csizg.core.R
import com.csizg.core.WxContactsReceiver
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val switcherAccess = isAccessibilitySettingsOn(this, AccessibilityService::class.java)
        if(!switcherAccess) {
            Toast.makeText(this, "请开启辅助功能", Toast.LENGTH_LONG).show()
            startActivity( Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        }

    }


    override fun onResume() {
        super.onResume()

        val txt = App.mApp.users.joinToString("\n"){it.nickName ?: ""}
        tv_contacts_list.text = txt
    }

    fun clearWechatContacts(view: View){
        App.mApp.users.clear()
        Toast.makeText(this, "微信昵称列表已清空", Toast.LENGTH_LONG).show()
        onResume()
    }

    private fun isAccessibilitySettingsOn(mContext: Context, clazz: Class<out AccessibilityService?>): Boolean {
        var accessibilityEnabled = 0
        val service: String = mContext.packageName.toString() + "/" + clazz.canonicalName
        try {
            accessibilityEnabled = Settings.Secure.getInt(mContext.applicationContext.contentResolver,
                    Settings.Secure.ACCESSIBILITY_ENABLED)
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
        }
        val mStringColonSplitter = SimpleStringSplitter(':')
        if (accessibilityEnabled == 1) {
            val settingValue: String = Settings.Secure.getString(mContext.applicationContext.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            mStringColonSplitter.setString(settingValue)
            while (mStringColonSplitter.hasNext()) {
                val accessibilityService = mStringColonSplitter.next()
                if (accessibilityService.equals(service, ignoreCase = true)) {
                    return true
                }
            }
        }
        return false
    }

}