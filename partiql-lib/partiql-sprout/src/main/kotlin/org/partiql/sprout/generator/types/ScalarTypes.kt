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

import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import org.partiql.sprout.model.ScalarType

object ScalarTypes {

    val bool: TypeName = Boolean::class.asTypeName()

    val int: TypeName = Int::class.asTypeName()

    val long: TypeName = Long::class.asTypeName()

    val float: TypeName = Float::class.asTypeName()

    val double: TypeName = Double::class.asTypeName()

    val bytes: TypeName = ByteArray::class.asTypeName()

    val string: TypeName = String::class.asTypeName()

    val primitives: Map<ScalarType, TypeName> = mapOf(
        ScalarType.BOOL to bool,
        ScalarType.INT to int,
        ScalarType.LONG to long,
        ScalarType.FLOAT to float,
        ScalarType.DOUBLE to double,
        ScalarType.BYTES to bytes,
        ScalarType.STRING to string,
    )

    fun typeNameOf(type: ScalarType) = primitives[type]!!
}
