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

package org.partiql.spi.plugins.local

import com.google.gson.Gson
import com.google.gson.stream.JsonReader

class LocalSchema(
    public val name: String,
    public val type: LocalObjectType,
    public val attributes: List<LocalColumnSchema>
) {
    companion object {
        @JvmStatic
        fun fromJson(json: String): LocalSchema {
            val reader = json.reader()
            val jsonReader = JsonReader(reader)
            return Gson().fromJson(jsonReader, LocalSchema::class.java)
        }
    }
}
