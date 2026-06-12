package com.lingjing.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.*
import com.lingjing.worker.*
import java.util.concurrent.TimeUnit

/**
 * 开机广播接收器 - 重启所有后台任务
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val wm = WorkManager.getInstance(context)
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            // 感知检测 - 每4小时
            wm.enqueueUniquePeriodicWork(
                "perception_check", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<PerceptionCheckWorker>(4, TimeUnit.HOURS)
                    .setConstraints(constraints).build()
            )
            // 真元恢复 - 每小时
            wm.enqueueUniquePeriodicWork(
                "energy_recovery", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<EnergyRecoveryWorker>(1, TimeUnit.HOURS)
                    .setConstraints(constraints).build()
            )
            // Widget 刷新 - 每30分钟
            wm.enqueueUniquePeriodicWork(
                "widget_refresh", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<WidgetRefreshWorker>(30, TimeUnit.MINUTES)
                    .setConstraints(constraints).build()
            )
        }
    }
}
