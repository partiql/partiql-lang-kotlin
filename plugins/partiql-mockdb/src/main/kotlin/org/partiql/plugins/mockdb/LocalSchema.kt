/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at:
 *
 *       http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.plugins.mockdb

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.stream.JsonReader
import java.lang.reflect.Type

sealed class LocalSchema {
    companion object {
        @JvmStatic
        fun fromJson(json: String): LocalSchema {
            val reader = json.reader()
            val jsonReader = JsonReader(reader)
            val gson = GsonBuilder().registerTypeAdapter(LocalSchema::class.java, Deserializer()).create()
            return gson.fromJson(jsonReader, LocalSchema::class.java)
        }
    }

    data class TableSchema(
        val name: String,
        val attributes: List<ValueSchema>
    ) : LocalSchema() {
        val type: String = "TABLE"
    }

    sealed class ValueSchema : LocalSchema() {
        data class StructSchema(
            val name: String,
            val attributes: List<ValueSchema>
        ) : ValueSchema() {
            val type: String = "STRUCT"
        }

        data class ScalarSchema(
            val name: String,
            val type: LocalObjectType,
            val attributes: List<Int>
        ) : ValueSchema()
    }

    class Deserializer : JsonDeserializer<LocalSchema> {
        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): LocalSchema? {
            val jsonObject = json?.asJsonObject ?: return null
            val name = jsonObject.get("name").asString
            val type = jsonObject.get("type").asString
            val remaining = jsonObject.getAsJsonArray("attributes")
            return when (type) {
                "TABLE" -> {
                    val children = remaining.map { deserialize(it, LocalSchema::class.java, context) as ValueSchema }
                    TableSchema(name, children)
                }
                "STRUCT" -> {
                    val children = remaining.map { deserialize(it, LocalSchema::class.java, context) as ValueSchema }
                    ValueSchema.StructSchema(name, children)
                }
                else -> {
                    val children = remaining.map { it.asInt }
                    ValueSchema.ScalarSchema(name, LocalObjectType.valueOf(type), children)
                }
            }
        }
    }
}
