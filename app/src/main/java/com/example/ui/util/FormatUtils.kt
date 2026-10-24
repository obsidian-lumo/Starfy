package com.example.ui.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {

    fun formatBytes(bytes: Long): String {
        val gigabyte = 1024L * 1024L * 1024L
        val megabyte = 1024L * 1024L
        val kilobyte = 1024L

        return when {
            bytes >= gigabyte -> String.format(Locale.getDefault(), "%.2f GB", bytes.toDouble() / gigabyte)
            bytes >= megabyte -> String.format(Locale.getDefault(), "%.1f MB", bytes.toDouble() / megabyte)
            bytes >= kilobyte -> String.format(Locale.getDefault(), "%.1f KB", bytes.toDouble() / kilobyte)
            else -> "$bytes B"
        }
    }

    fun formatSpeed(
        bytesPerSec: Long,
        speedUnit: com.example.ui.theme.SpeedUnit = com.example.ui.theme.SpeedUnit.BYTES
    ): String {
        return if (speedUnit == com.example.ui.theme.SpeedUnit.BITS) {
            val bitsPerSec = bytesPerSec * 8.0
            val megabit = 1_000_000.0
            val kilobit = 1_000.0
            when {
                bitsPerSec >= megabit -> String.format(Locale.getDefault(), "%.1f Мбит/с", bitsPerSec / megabit)
                bitsPerSec >= kilobit -> String.format(Locale.getDefault(), "%.0f Кбит/с", bitsPerSec / kilobit)
                else -> "${bitsPerSec.toLong()} бит/с"
            }
        } else {
            val megabyte = 1024L * 1024L
            val kilobyte = 1024L
            when {
                bytesPerSec >= megabyte -> String.format(Locale.getDefault(), "%.1f MB/s", bytesPerSec.toDouble() / megabyte)
                bytesPerSec >= kilobyte -> String.format(Locale.getDefault(), "%.0f KB/s", bytesPerSec.toDouble() / kilobyte)
                else -> "$bytesPerSec B/s"
            }
        }
    }

    fun formatDuration(seconds: Long): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hrs > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
        }
    }

    fun formatTimestamp(epochSeconds: Long): String {
        if (epochSeconds <= 0) return "Бессрочно"
        val date = Date(epochSeconds * 1000)
        val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("ru"))
        return sdf.format(date)
    }

    fun formatDaysRemaining(epochSeconds: Long): String {
        val nowSec = System.currentTimeMillis() / 1000
        val diffSec = epochSeconds - nowSec
        if (diffSec <= 0) return "Истекла"
        val days = diffSec / 86400
        return when {
            days > 365 -> "${days / 365} г. ${days % 365} дн."
            else -> "$days дней"
        }
    }
}
