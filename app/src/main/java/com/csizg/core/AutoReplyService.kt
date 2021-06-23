package com.csizg.core

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Notification
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.csizg.core.AsCore.BROADCAST_ACTION


class AutoReplyService : AccessibilityService() {
    val TAG = AutoReplyService::class.java.simpleName

    private var toConfirmNickName: String? = null
    private val users: LinkedHashSet<WxUser> = LinkedHashSet()

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val eventType = event.eventType
        Log.d(TAG, "get event = $eventType event_class = ${event.className}")
        when (eventType) {
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                if (event.text.isNotEmpty()) {
                    (event.parcelableData as? Notification)?.let {
                        val notification = event.parcelableData as Notification
                        val content = notification.tickerText.toString()
                        val cc = content.split(":".toRegex()).toTypedArray()
                        if(cc.isEmpty() || cc.size < 2){
                            return
                        }
                        // todo 预留接口 监听通知栏消息
                        val name = cc[0].trim { it <= ' ' }
                        val sContent = cc[1].trim { it <= ' ' }
                        Log.i(TAG, "sender name =$name $sContent")
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
                        val ivHeadNodes = rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/au2")
                        if (ivHeadNodes.isNotEmpty()) {
                            val wxUser = users.find { it.remarkName == nickOrRemarkName }
                            if (wxUser == null) {
                                Toast.makeText(this, "牛盾管家关联微信用户名", Toast.LENGTH_SHORT).show()



                                showPop(ivHeadNodes)


                                toConfirmNickName = nickOrRemarkName
                                users.add(WxUser(nickOrRemarkName, nickOrRemarkName))
                                toUserDetail(nickOrRemarkName, ivHeadNodes)
                            } else {
                                toConfirmNickName = null
                                Toast.makeText(this, wxUser.nickName, Toast.LENGTH_SHORT).show()
                                users.remove(wxUser)
                                users.add(wxUser)
                            }
                        }

                        sendWxContactsBroadcast()
                    }
                }
                if (className == "com.tencent.mm.plugin.profile.ui.ContactInfoUI") {
                    val tvNodes = rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/bd1")
                    var nickText: String? = null
                    if (tvNodes.isNotEmpty()) {
                        val tvNickName = tvNodes[0]
                        if (isTextView(tvNickName)) {
                            val nickArr = tvNickName.text.toString().split(":".toRegex()).toTypedArray()
                            if(nickArr.size > 1) {
                                nickText = nickArr[1].trim()
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

    private fun showPop(nodes:List<AccessibilityNodeInfo>) {
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val layoutParams = WindowManager.LayoutParams();
        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        layoutParams.flags =  WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_FULLSCREEN
        layoutParams.format = PixelFormat.TRANSLUCENT
        layoutParams.gravity = Gravity.TOP

        val view = LayoutInflater.from(this).inflate(R.layout.pop_window,null);

        windowManager.addView(view,layoutParams)

        Handler(Looper.getMainLooper()).postDelayed({
            windowManager.removeView(view)
        },1000)
    }


    /**
     * 找到对应的好友并且点击好友的微信头像
     *
     */
    private fun toUserDetail(destName: String, ivHeadNodes: List<AccessibilityNodeInfo>) {
        for (ivHead in ivHeadNodes) {
            val label = ivHead.contentDescription.replace("头像".toRegex(), "").trim()
            if (destName == label) {
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

    override fun onServiceConnected() {
        super.onServiceConnected()
        // 代码和xml都需要配置否则不同手机不兼容！！！
        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED or  AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.notificationTimeout = 100
        serviceInfo = info
    }
}