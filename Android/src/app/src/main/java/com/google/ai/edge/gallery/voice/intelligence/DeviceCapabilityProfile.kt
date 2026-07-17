package com.google.ai.edge.gallery.voice.intelligence

import android.app.ActivityManager
import android.content.Context
import android.os.BatteryManager
import android.os.PowerManager
import kotlin.math.ceil

data class DeviceCapabilityProfile(
  val totalMemoryGb: Int,
  val batteryPercent: Int,
  val thermalStatus: Int,
) {
  val isLowBattery: Boolean
    get() = batteryPercent in 0..19

  val isThermallyConstrained: Boolean
    get() = thermalStatus >= PowerManager.THERMAL_STATUS_SEVERE

  val allowsDeepThinking: Boolean
    get() = !isLowBattery && !isThermallyConstrained
}

class DeviceCapabilityProfileProvider(private val context: Context) {
  fun current(): DeviceCapabilityProfile {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memoryInfo = ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)
    // Android reports binary GiB; rounding up preserves the device's marketed RAM category.
    val totalMemoryGb =
      ceil(memoryInfo.totalMem.toDouble() / BYTES_PER_GB).toInt().coerceAtLeast(1)
    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    val batteryPercent = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return DeviceCapabilityProfile(
      totalMemoryGb = totalMemoryGb,
      batteryPercent = batteryPercent,
      thermalStatus = powerManager.currentThermalStatus,
    )
  }

  companion object {
    private const val BYTES_PER_GB = 1024L * 1024L * 1024L
  }
}
