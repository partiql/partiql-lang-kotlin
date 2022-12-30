/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *     
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.partiql.sprout.generator.target.kotlin.types

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName

object JacksonTypes {

    private const val root = "com.fasterxml.jackson"

    object Core {

        const val packageName = "$root.core"

        val jsonParser = ClassName(packageName, "JsonParser")
    }

    object Databind {

        const val packageName = "$root.databind"

        val objectMapper = ClassName(packageName, "ObjectMapper")

        val simpleModule = ClassName("$packageName.module", "SimpleModule")

        val jsonDeserializer = ClassName(packageName, "JsonDeserializer")

        val jsonNode = ClassName(packageName, "JsonNode")

        val deserializationContext = ClassName(packageName, "DeserializationContext")
    }

    object Annotation {

        const val packageName = "$root.annotation"

        val ignoreProperties = ClassName(packageName, "JsonIgnoreProperties")

        val property = ClassName(packageName, "JsonProperty")

        val propertyOrder = ClassName(packageName, "JsonPropertyOrder")

        fun ignore(members: Iterable<String>) = AnnotationSpec.builder(ignoreProperties)
            .addMember(members.joinToString { "\"$it\"" })
            .build()

        fun property(name: String) = AnnotationSpec.builder(property)
            .addMember("%S", name)
            .build()

        fun order(vararg members: String) = AnnotationSpec.builder(propertyOrder)
            .addMember(members.joinToString { "\"$it\"" })
            .build()
    }
}
