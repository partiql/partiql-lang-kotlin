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

import org.partiql.lang.*
import org.partiql.lang.errors.Property.*
import org.partiql.lang.syntax.TokenType
import org.junit.Before
import org.junit.Test


class PropertyValueMapTest : TestBase() {

    val emptyValueMap: PropertyValueMap = PropertyValueMap()
    val onlyColumnValueMap: PropertyValueMap = PropertyValueMap()
    val oneOfEachType: PropertyValueMap = PropertyValueMap()

    @Before
    fun setUp() {
        onlyColumnValueMap[COLUMN_NUMBER] = 11L
        oneOfEachType[EXPECTED_TOKEN_TYPE] = TokenType.COMMA
        oneOfEachType[KEYWORD] = "test"
        oneOfEachType[EXPECTED_ARITY_MAX] = 1
        oneOfEachType[TOKEN_VALUE] = ion.newEmptyList()
        oneOfEachType[COLUMN_NUMBER] = 11L
    }

    @Test fun getPropFromEmptyBag() {
        assertNull(emptyValueMap[LINE_NUMBER])
    }

    @Test fun getAbsentPropFromNonEmptyBag() {
        assertNull(onlyColumnValueMap[LINE_NUMBER])
    }


    @Test fun getValues() {
        assertEquals(11L, oneOfEachType[COLUMN_NUMBER]?.longValue())
        assertEquals(TokenType.COMMA, oneOfEachType[EXPECTED_TOKEN_TYPE]?.tokenTypeValue())
        assertEquals("test", oneOfEachType[KEYWORD]?.stringValue())
        assertEquals(1, oneOfEachType[EXPECTED_ARITY_MAX]?.integerValue())
        assertEquals(11L, oneOfEachType[COLUMN_NUMBER]?.longValue())
    }

}