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

package org.partiql.lang.ast

import com.amazon.ion.system.*
import org.junit.*
import org.junit.Test
import kotlin.test.*

class SourceLocationMetaTest {
    @Test
    fun test1() {
        val ion = IonSystemBuilder.standard().build()

        val sl = SourceLocationMeta(1, 2, 3)
        val expected = ion.singleValue("{ line_num: 1, char_offset: 2, length: 3 }")

        val dg = ion.newDatagram()
        val writer = ion.newWriter(dg)
        sl.serialize(writer)
        val result = dg.first()
        assertEquals(expected, result)
    }
}