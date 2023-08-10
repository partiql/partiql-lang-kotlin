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

package org.partiql.lang.errors

import org.junit.Before
import org.junit.Test
import org.partiql.errors.Property
import org.partiql.errors.PropertyValueMap
import org.partiql.lang.TestBase

class PropertyValueMapTest : TestBase() {

    val emptyValueMap: PropertyValueMap = PropertyValueMap()
    val onlyColumnValueMap: PropertyValueMap = PropertyValueMap()
    val oneOfEachType: PropertyValueMap = PropertyValueMap()

    @Before
    fun setUp() {
        onlyColumnValueMap[Property.COLUMN_NUMBER] = 11L
        oneOfEachType[Property.EXPECTED_ARITY_MAX] = 1
        oneOfEachType[Property.TOKEN_VALUE] = ion.newEmptyList()
        oneOfEachType[Property.COLUMN_NUMBER] = 11L
    }

    @Test fun getPropFromEmptyBag() {
        assertNull(emptyValueMap[Property.LINE_NUMBER])
    }

    @Test fun getAbsentPropFromNonEmptyBag() {
        assertNull(onlyColumnValueMap[Property.LINE_NUMBER])
    }

    @Test fun getValues() {
        assertEquals(11L, oneOfEachType[Property.COLUMN_NUMBER]?.longValue())
        assertEquals(1, oneOfEachType[Property.EXPECTED_ARITY_MAX]?.integerValue())
        assertEquals(11L, oneOfEachType[Property.COLUMN_NUMBER]?.longValue())
    }
}
