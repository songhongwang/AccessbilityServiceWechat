package com.csizg.core

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.csizg.core.AsCore.BROADCAST_ACTION
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashSet

class AutoReplyService : AccessibilityService() {
    private var toConfirmNickName: String? = null
    private val users: LinkedHashSet<WxUser> = LinkedHashSet()

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val eventType = event.eventType
        Log.d(
            "maptrix",
            "get event = " + eventType + "eventclass = " + event.className.toString()
        )
        when (eventType) {
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                Log.d("maptrix", "get notification event")
                val texts = event.text
                if (texts.isNotEmpty()) {
                    if (event.parcelableData != null
                        && event.parcelableData is Notification
                    ) {
                        val notification = event.parcelableData as Notification
                        val content = notification.tickerText.toString()
                        val cc = content.split(":".toRegex()).toTypedArray()
                        val name = cc[0].trim { it <= ' ' }
                        val scontent = cc[1].trim { it <= ' ' }
                        Log.i("maptrix", "sender name =$name")
                        Log.i("maptrix", "sender content =$scontent")
                    }
                }
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val className = event.className.toString()
                val rootInActiveWindow = rootInActiveWindow
                if (className == "com.tencent.mm.ui.LauncherUI" || className == "android.widget.LinearLayout") {

                    // 获取好友名字（微信昵称）
                    val nickNodes =
                        rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/ipt")
                    if (nickNodes.isEmpty()) {
                        return
                    }
                    val tvNickName = nickNodes[0]
                    if (isTextView(tvNickName)) {
                        val nickOrRemarkName = tvNickName.text.toString()

                        // 点击好友头像进入用户详情页面获取用户的昵称（不是备注是微信名）
                        val ivHeadNodes =
                            rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/au2")
                        if (ivHeadNodes.isNotEmpty()) {
                            val wxUser = users.find { it.remarkName == nickOrRemarkName }
                            if (wxUser == null) {
                                Toast.makeText(this, "牛盾管家关联微信用户名", Toast.LENGTH_SHORT).show()
                                toConfirmNickName = nickOrRemarkName
                                users.add(WxUser(nickOrRemarkName, nickOrRemarkName))
                                toUserDetail(nickOrRemarkName, ivHeadNodes)
                            } else {
                                toConfirmNickName = null
                                Toast.makeText(this, wxUser.nickName, Toast.LENGTH_SHORT).show()
                                Log.d("maptrix", "昵称： " + wxUser.nickName)
                                users.remove(wxUser)
                                users.add(wxUser)
                            }
                        }

                        sendWxContactsBroadcast()
                    }
                }
                if (className == "com.tencent.mm.plugin.profile.ui.ContactInfoUI") {
                    val tvNodes =
                        rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/bd1")
                    var nickText: String? = null
                    if (!tvNodes.isEmpty()) {
                        val tvNickName = tvNodes[0]
                        if (isTextView(tvNickName)) {
                            nickText = tvNickName.text.toString()
                            if (nickText.contains(":")) { // 文本内容 昵称:  宋红旺
                                if (nickText.indexOf(":") + 3 < nickText.length) {
                                    nickText = nickText.substring(nickText.indexOf(":") + 3)
                                }
                            }
                        }
                    }
                    if (toConfirmNickName != null) {
                        val wxUser = users.find { it.remarkName == toConfirmNickName }
                        if (wxUser != null && nickText != null) {
                            wxUser.nickName = nickText
                            users.add(wxUser)
                        }
                        toConfirmNickName = null
                        performBack(rootInActiveWindow)
                    }
                }
            }
        }
    }

    private fun toUserDetail(
        nickOrRemarkName: String,
        ivHeadNodes: List<AccessibilityNodeInfo>
    ) {
        for (ivHead in ivHeadNodes) {
            val contentDescription = ivHead.contentDescription ?: continue
            if (contentDescription.toString().indexOf("头像") > contentDescription.length) {
                continue
            }
            val label = contentDescription.toString()
                .substring(0, contentDescription.toString().indexOf("头像"))
            if (nickOrRemarkName == label) {
                ivHead.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                break
            }
        }
    }

    private fun performBack(rootNode: AccessibilityNodeInfo) {
        val backNodes = rootNode.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/eh")
        backNodes[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    override fun onInterrupt() {}
    private fun sendWxContactsBroadcast() {
        val intent = Intent()
        intent.action = BROADCAST_ACTION

        val list = ArrayList<WxUser>(users.reversed().take(10))

        intent.putParcelableArrayListExtra("wxContactsList", list)
        sendBroadcast(intent)
    }

    /**
     * 回到系统桌面
     */
    private fun back2Home() {
        val home = Intent(Intent.ACTION_MAIN)
        home.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        home.addCategory(Intent.CATEGORY_HOME)
        startActivity(home)
    }

    private fun isButton(node: AccessibilityNodeInfo): Boolean {
        return node.className.toString() == "android.widget.Button"
    }

    private fun isTextView(node: AccessibilityNodeInfo): Boolean {
        return node.className.toString() == "android.widget.TextView"
    }
}