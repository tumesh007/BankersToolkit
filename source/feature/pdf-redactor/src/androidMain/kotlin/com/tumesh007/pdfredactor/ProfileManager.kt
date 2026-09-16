package com.tumesh007.pdfredactor

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class RedactionProfile(
    val name: String,
    val regions: List<NormalizedRect>,
    val pageSelection: String = "all",
    val pageSpec: String = "",
    val colorRgb: Triple<Float, Float, Float> = Triple(0f, 0f, 0f),
    val opacity: Float = 1.0f,
    val margin: Float = 0.005f,
    val schemaVersion: Int = 1
) {
    fun toJson(): String {
        val root = JSONObject()
        root.put("name", name)
        root.put("schema_version", schemaVersion)
        root.put("page_selection", pageSelection)
        root.put("page_spec", pageSpec)
        root.put("opacity", opacity.toDouble())
        root.put("margin", margin.toDouble())

        val colorArr = JSONArray().apply {
            put(colorRgb.first.toDouble())
            put(colorRgb.second.toDouble())
            put(colorRgb.third.toDouble())
        }
        root.put("color", colorArr)

        val regionsArr = JSONArray()
        for (r in regions) {
            val rObj = JSONObject()
            rObj.put("x", r.x.toDouble())
            rObj.put("y", r.y.toDouble())
            rObj.put("width", r.width.toDouble())
            rObj.put("height", r.height.toDouble())
            regionsArr.put(rObj)
        }
        root.put("regions", regionsArr)
        return root.toString(2)
    }

    companion object {
        fun fromJson(jsonStr: String): RedactionProfile {
            val root = JSONObject(jsonStr)
            val name = root.optString("name", "Untitled")
            val schemaVersion = root.optInt("schema_version", 1)
            val pageSelection = root.optString("page_selection", "all")
            val pageSpec = root.optString("page_spec", "")
            val opacity = root.optDouble("opacity", 1.0).toFloat()
            val margin = root.optDouble("margin", 0.005).toFloat()

            val colorArr = root.optJSONArray("color")
            val r = colorArr?.optDouble(0, 0.0)?.toFloat() ?: 0f
            val g = colorArr?.optDouble(1, 0.0)?.toFloat() ?: 0f
            val b = colorArr?.optDouble(2, 0.0)?.toFloat() ?: 0f

            val regionsList = mutableListOf<NormalizedRect>()
            val regionsArr = root.optJSONArray("regions")
            if (regionsArr != null) {
                for (i in 0 until regionsArr.length()) {
                    val rObj = regionsArr.getJSONObject(i)
                    regionsList.add(
                        NormalizedRect(
                            x = rObj.getDouble("x").toFloat(),
                            y = rObj.getDouble("y").toFloat(),
                            width = rObj.getDouble("width").toFloat(),
                            height = rObj.getDouble("height").toFloat()
                        )
                    )
                }
            }
            return RedactionProfile(
                name = name,
                regions = regionsList,
                pageSelection = pageSelection,
                pageSpec = pageSpec,
                colorRgb = Triple(r, g, b),
                opacity = opacity,
                margin = margin,
                schemaVersion = schemaVersion
            )
        }
    }
}

object ProfileManager {
    fun saveProfile(profile: RedactionProfile, dir: File) {
        dir.mkdirs()
        val sanitized = profile.name.replace(Regex("""[^a-zA-Z0-9_\-]"""), "_")
        val file = File(dir, "$sanitized.json")
        file.writeText(profile.toJson())
    }

    fun loadProfile(file: File): RedactionProfile {
        return RedactionProfile.fromJson(file.readText())
    }

    fun listProfiles(dir: File): List<RedactionProfile> {
        if (!dir.exists()) return emptyList()
        return dir.listFiles { _, name -> name.endsWith(".json") }
            ?.mapNotNull {
                try { loadProfile(it) } catch (_: Exception) { null }
            } ?: emptyList()
    }
}
