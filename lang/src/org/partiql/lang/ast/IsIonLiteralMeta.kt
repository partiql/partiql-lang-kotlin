/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

/*

 */
package org.partiql.lang.ast

import com.amazon.ion.*
import org.partiql.lang.ast.*

/**
 * Meta node intended to be attached to an instance of [Literal] to indicate that it was
 * designated as an `ionLiteral` in the parsed statement.
 */
class IsIonLiteralMeta private constructor(): Meta {
    override val tag = TAG

    companion object {
        const val TAG = "\$is_ion_literal"

        val instance = IsIonLiteralMeta()
        val deserializer = MemoizedMetaDeserializer(TAG, instance)
    }
}