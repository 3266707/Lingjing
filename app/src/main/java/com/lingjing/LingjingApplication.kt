package com.lingjing

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Environment
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

@HiltAndroidApp
class LingjingApplication : Application() {

    companion object {
        const val TAG = "Lingjing"
        const val CHANNEL_REMINDER = "lingjing_reminder"
        const val CHANNEL_ENCOURAGE = "lingjing_encourage"
        const val CHANNEL_ACHIEVEMENT = "lingjing_achievement"
    }

    override fun onCreate() {
        super.onCreate()

        // ====== 崩溃日志记录 ======
        val crashLogDir = File(getExternalFilesDir(null), "crash_logs")
        crashLogDir.mkdirs()

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // 写入文件
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val crashFile = File(crashLogDir, "crash_$timestamp.txt")

                FileWriter(crashFile).use { writer ->
                    val pw = PrintWriter(writer)
                    pw.println("========== 灵境 崩溃报告 ==========")
                    pw.println("时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())}")
                    pw.println("线程: ${thread.name}")
                    pw.println("Android: ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})")
                    pw.println("设备: ${Build.MANUFACTURER} ${Build.MODEL}")
                    pw.println()
                    pw.println("--- 异常堆栈 ---")
                    throwable.printStackTrace(pw)
                    pw.println()
                    pw.println("--- 原因链 ---")
                    var cause: Throwable? = throwable.cause
                    while (cause != null) {
                        pw.println("Caused by: ${cause.javaClass.name}: ${cause.message}")
                        cause.printStackTrace(pw)
                        cause = cause.cause
                        pw.println()
                    }
                    pw.flush()
                }
                Log.e(TAG, "崩溃日志已写入: ${crashFile.absolutePath}")
            } catch (e: Exception) {
                Log.e(TAG, "写入崩溃日志失败", e)
            }

            // 交给系统默认处理器（显示"已停止运行"对话框）
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // ====== 应用启动日志 ======
        try {
            val startupLog = File(crashLogDir, "startup_log.txt")
            FileWriter(startupLog, true).use { writer ->
                writer.write("[${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}] App onCreate 开始\n")
            }
        } catch (_: Exception) {}

        try {
            createNotificationChannels()
            logStartup("通知渠道创建成功")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create notification channels", e)
            logStartup("通知渠道创建失败: ${e.message}")
        }

        logStartup("App 初始化完成")
    }

    private fun logStartup(msg: String) {
        try {
            val logDir = File(getExternalFilesDir(null), "crash_logs")
            FileWriter(File(logDir, "startup_log.txt"), true).use { writer ->
                writer.write("[${SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())}] $msg\n")
            }
        } catch (_: Exception) {}
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_REMINDER,
                    getString(R.string.notification_channel_reminder),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "修炼提醒" },
                NotificationChannel(
                    CHANNEL_ENCOURAGE,
                    getString(R.string.notification_channel_encourage),
                    NotificationManager.IMPORTANCE_LOW
                ).apply { description = "道心鼓舞" },
                NotificationChannel(
                    CHANNEL_ACHIEVEMENT,
                    getString(R.string.notification_channel_achievement),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = "机缘成就" }
            )

            val manager = getSystemService(NotificationManager::class.java)
            channels.forEach { manager.createNotificationChannel(it) }
        }
    }
}
