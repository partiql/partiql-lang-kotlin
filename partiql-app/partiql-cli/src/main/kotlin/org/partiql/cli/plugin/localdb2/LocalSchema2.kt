package org.partiql.cli.plugin.localdb2

import com.google.gson.Gson
import com.google.gson.stream.JsonReader

class LocalSchema2(
    public val name: String,
    public val attributes: List<LocalColumnSchema2>
) {
    companion object {
        @JvmStatic
        fun fromJson(json: String): LocalSchema2 {
            val reader = json.reader()
            val jsonReader = JsonReader(reader)
            return Gson().fromJson<LocalSchema2>(jsonReader, LocalSchema2::class.java)
        }
    }
}
