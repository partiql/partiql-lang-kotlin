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

package org.partiql.sprout.generator.types

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asTypeName

object KotlinTypes {

    val R = TypeVariableName("R")

    val C = TypeVariableName("C")

    val T = TypeVariableName("T")

    val `R?` = TypeVariableName("R?")

    val `C?` = TypeVariableName("C?")

    val `T?` = TypeVariableName("T?")

    val list = ClassName("kotlin.collections", "List")

    val set = ClassName("kotlin.collections", "Set")

    val map = ClassName("kotlin.collections", "Map")

    val mutableMap = ClassName("kotlin.collections", "MutableMap")

    val any = Any::class.asTypeName()

    val `any?` = Any::class.asTypeName().copy(true)

    val nothing = ClassName("kotlin", "Nothing")
}
