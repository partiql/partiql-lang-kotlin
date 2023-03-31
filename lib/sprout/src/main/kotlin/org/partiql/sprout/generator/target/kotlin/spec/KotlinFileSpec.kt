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

package org.partiql.sprout.generator.target.kotlin.spec

import java.io.IOException

/**
 * For now, this just inverts the KotlinPoet FileSpec dependency
 */
class KotlinFileSpec internal constructor(
    private val file: com.squareup.kotlinpoet.FileSpec
) {

    val name: String = file.name

    val packageName: String = file.packageName

    @Throws(IOException::class)
    fun writeTo(out: Appendable) {
        file.writeTo(out)
    }
}
