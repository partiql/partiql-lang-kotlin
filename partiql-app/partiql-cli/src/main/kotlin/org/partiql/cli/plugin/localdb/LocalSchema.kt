package org.partiql.cli.plugin.localdb

import com.google.gson.Gson
import com.google.gson.stream.JsonReader

class LocalSchema(
    public val name: String,
    public val attributes: List<LocalColumnSchema>
) {
    companion object {
        @JvmStatic
        fun fromJson(json: String): LocalSchema {
            val reader = json.reader()
            val jsonReader = JsonReader(reader)
            return Gson().fromJson<LocalSchema>(jsonReader, LocalSchema::class.java)
        }
    }
}
