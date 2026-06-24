package com.salesinventory.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class ReportSettingsManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("report_settings", Context.MODE_PRIVATE)

    fun load(): ReportSettings {
        val json = prefs.getString("settings", null) ?: return ReportSettings()
        return try {
            val obj = JSONObject(json)
            val sections = mutableListOf<ReportCustomSection>()
            val arr = obj.optJSONArray("customSections")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val s = arr.getJSONObject(i)
                    sections.add(ReportCustomSection(
                        id = s.getString("id"),
                        title = s.getString("title"),
                        content = s.getString("content")
                    ))
                }
            }
            ReportSettings(
                storeName = obj.optString("storeName", ""),
                monthlyTarget = obj.optDouble("monthlyTarget", 0.0),
                minStockThreshold = obj.optInt("minStockThreshold", 5),
                showDailySales = obj.optBoolean("showDailySales", true),
                showMTD = obj.optBoolean("showMTD", true),
                showTarget = obj.optBoolean("showTarget", true),
                showStockLevel = obj.optBoolean("showStockLevel", true),
                showGeneratedTime = obj.optBoolean("showGeneratedTime", true),
                enableSubGrouping = obj.optBoolean("enableSubGrouping", true),
                subGroupSuffixes = obj.optString("subGroupSuffixes", "MS,CS,LS"),
                customSections = sections
            )
        } catch (e: Exception) {
            ReportSettings()
        }
    }

    fun save(settings: ReportSettings) {
        val obj = JSONObject()
        obj.put("storeName", settings.storeName)
        obj.put("monthlyTarget", settings.monthlyTarget)
        obj.put("minStockThreshold", settings.minStockThreshold)
        obj.put("showDailySales", settings.showDailySales)
        obj.put("showMTD", settings.showMTD)
        obj.put("showTarget", settings.showTarget)
        obj.put("showStockLevel", settings.showStockLevel)
        obj.put("showGeneratedTime", settings.showGeneratedTime)
        obj.put("enableSubGrouping", settings.enableSubGrouping)
        obj.put("subGroupSuffixes", settings.subGroupSuffixes)
        val arr = JSONArray()
        settings.customSections.forEach { section ->
            val s = JSONObject()
            s.put("id", section.id)
            s.put("title", section.title)
            s.put("content", section.content)
            arr.put(s)
        }
        obj.put("customSections", arr)
        prefs.edit().putString("settings", obj.toString()).apply()
    }
}
