package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SubscriptionRepository
import com.example.ui.theme.AppThemeColor
import com.example.ui.util.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Twelve VPN", appName)
    }

    @Test
    fun `subscription repository can parse servers from json`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = SubscriptionRepository(context)
        val servers = repository.defaultServers
        assertTrue(servers.isNotEmpty())
        assertTrue(servers.any { it.name.contains("Вена") })
        assertTrue(servers.any { it.protocol == "VLESS" })
    }

    @Test
    fun `theme colors contain all available palettes and respect dark-light filtering`() {
        val colors = AppThemeColor.entries
        assertEquals(12, colors.size)
        assertTrue(colors.contains(AppThemeColor.MONOCHROME_WHITE))
        assertTrue(colors.contains(AppThemeColor.MONOCHROME_BLACK))
        assertTrue(colors.contains(AppThemeColor.CYAN))
        assertTrue(colors.contains(AppThemeColor.EMERALD))
        assertTrue(colors.contains(AppThemeColor.CRIMSON))
        assertTrue(colors.contains(AppThemeColor.GOLD))

        // Dark theme hides black palette and maps MONOCHROME_BLACK -> MONOCHROME_WHITE
        val darkAvailable = colors.filter { !it.isDarkSwatch }
        assertFalse(darkAvailable.contains(AppThemeColor.MONOCHROME_BLACK))
        assertTrue(darkAvailable.contains(AppThemeColor.MONOCHROME_WHITE))
        assertEquals(AppThemeColor.MONOCHROME_WHITE, AppThemeColor.MONOCHROME_BLACK.compatibleWith(isDarkTheme = true))

        // Light theme hides white palette and maps MONOCHROME_WHITE -> MONOCHROME_BLACK
        val lightAvailable = colors.filter { !it.isLightSwatch }
        assertFalse(lightAvailable.contains(AppThemeColor.MONOCHROME_WHITE))
        assertTrue(lightAvailable.contains(AppThemeColor.MONOCHROME_BLACK))
        assertEquals(AppThemeColor.MONOCHROME_BLACK, AppThemeColor.MONOCHROME_WHITE.compatibleWith(isDarkTheme = false))
    }

    @Test
    fun `format utils converts bytes and speed correctly`() {
        val formattedBytes = FormatUtils.formatBytes(175383337052L)
        assertTrue(formattedBytes.contains("GB"))

        val formattedSpeedBytes = FormatUtils.formatSpeed(5_200_000L, com.example.ui.theme.SpeedUnit.BYTES)
        assertTrue(formattedSpeedBytes.contains("MB/s"))

        val formattedSpeedBits = FormatUtils.formatSpeed(5_200_000L, com.example.ui.theme.SpeedUnit.BITS)
        assertTrue(formattedSpeedBits.contains("Мбит/с"))
    }
}
