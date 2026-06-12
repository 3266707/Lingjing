package com.lingjing.core.common.util

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * 日期时间工具类
 */
object DateTimeUtils {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val chineseDateFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日")

    fun today(): String = LocalDate.now().format(dateFormatter)

    fun now(): String = LocalDateTime.now().format(dateTimeFormatter)

    fun formatChinese(date: String): String {
        return try {
            LocalDate.parse(date, dateFormatter).format(chineseDateFormatter)
        } catch (e: Exception) {
            date
        }
    }

    fun daysAgo(days: Int): String {
        return LocalDate.now().minusDays(days.toLong()).format(dateFormatter)
    }

    fun daysFromNow(days: Int): String {
        return LocalDate.now().plusDays(days.toLong()).format(dateFormatter)
    }

    fun dateRange(startDaysAgo: Int, endDaysAgo: Int = 0): Pair<String, String> {
        val end = LocalDate.now().minusDays(endDaysAgo.toLong())
        val start = LocalDate.now().minusDays(startDaysAgo.toLong())
        return start.format(dateFormatter) to end.format(dateFormatter)
    }

    fun daysBetween(date1: String, date2: String): Long {
        return try {
            val d1 = LocalDate.parse(date1, dateFormatter)
            val d2 = LocalDate.parse(date2, dateFormatter)
            kotlin.math.abs(d1.toEpochDay() - d2.toEpochDay())
        } catch (e: Exception) {
            0
        }
    }

    fun isToday(date: String): Boolean = date == today()

    fun isYesterday(date: String): Boolean = date == daysAgo(1)

    fun getDayOfWeek(date: String): String {
        return try {
            val localDate = LocalDate.parse(date, dateFormatter)
            val dayOfWeek = localDate.dayOfWeek
            when (dayOfWeek) {
                java.time.DayOfWeek.MONDAY -> "周一"
                java.time.DayOfWeek.TUESDAY -> "周二"
                java.time.DayOfWeek.WEDNESDAY -> "周三"
                java.time.DayOfWeek.THURSDAY -> "周四"
                java.time.DayOfWeek.FRIDAY -> "周五"
                java.time.DayOfWeek.SATURDAY -> "周六"
                java.time.DayOfWeek.SUNDAY -> "周日"
            }
        } catch (e: Exception) {
            ""
        }
    }

    fun getWeekRange(date: String): Pair<String, String> {
        return try {
            val localDate = LocalDate.parse(date, dateFormatter)
            val monday = localDate.with(java.time.DayOfWeek.MONDAY)
            val sunday = localDate.with(java.time.DayOfWeek.SUNDAY)
            monday.format(dateFormatter) to sunday.format(dateFormatter)
        } catch (e: Exception) {
            date to date
        }
    }

    fun formatTimestamp(timestamp: Long): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            sdf.format(Date(timestamp))
        } catch (e: Exception) {
            timestamp.toString()
        }
    }

    fun formatTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        return when {
            diff < 60_000 -> "刚刚"
            diff < 3_600_000 -> "${diff / 60_000}分钟前"
            diff < 86_400_000 -> "${diff / 3_600_000}小时前"
            diff < 604_800_000 -> "${diff / 86_400_000}天前"
            else -> formatTimestamp(timestamp)
        }
    }
}
