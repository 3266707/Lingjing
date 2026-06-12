package com.lingjing.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 通知操作按钮接收器
 */
class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_COMPLETE_TASK = "com.lingjing.action.COMPLETE_TASK"
        const val ACTION_START_REST = "com.lingjing.action.START_REST"
        const val ACTION_DISMISS = "com.lingjing.action.DISMISS"
        const val EXTRA_TASK_ID = "extra_task_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Phase 4: 处理通知按钮点击
        // 由 PerceptionEngine 和 NotificationDispatcher 负责
    }
}
