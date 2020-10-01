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

package org.partiql.lang.eval

import org.partiql.lang.errors.*
import org.partiql.lang.util.*
import junitparams.*
import org.assertj.core.api.Assertions.*
import org.junit.*
import org.junit.Test
import org.junit.runner.*
import kotlin.test.*

@RunWith(JUnitParamsRunner::class)
class NodeMetadataTest {

    private fun assertEqualsErrorContext(expected: PropertyValueMap, actual: PropertyValueMap) {
        assertThat(expected.getProperties()).isEqualTo(actual.getProperties())

        softAssert {
            expected.getProperties().forEach { property ->
                val expectedValue = expected[property]!!
                val actualValue = actual[property]!!

                assertThat(expectedValue.type).isEqualTo(actualValue.type)
                assertThat(expectedValue.valueAsAny()).isEqualTo(actualValue.valueAsAny())
            }
        }
    }

    private fun PropertyValue.valueAsAny(): Any = when (this.type) {
        PropertyType.LONG_CLASS      -> this.longValue()
        PropertyType.STRING_CLASS    -> this.stringValue()
        PropertyType.INTEGER_CLASS   -> this.integerValue()
        PropertyType.TOKEN_CLASS     -> this.tokenTypeValue()
        PropertyType.ION_VALUE_CLASS -> this.ionValue()
    }

    private fun PropertyValueMap.clone(): PropertyValueMap = this.getProperties().fold(PropertyValueMap()) { acc, property ->
        when (property.propertyType) {
            PropertyType.LONG_CLASS      -> acc[property] = this[property]!!.longValue()
            PropertyType.STRING_CLASS    -> acc[property] = this[property]!!.stringValue()
            PropertyType.INTEGER_CLASS   -> acc[property] = this[property]!!.integerValue()
            PropertyType.TOKEN_CLASS     -> acc[property] = this[property]!!.tokenTypeValue()
            PropertyType.ION_VALUE_CLASS -> acc[property] = this[property]!!.ionValue()
        }

        acc
    }

    private fun buildErrorContext(initial: PropertyValueMap? = null,
                                  build: PropertyValueMap.() -> Unit): PropertyValueMap {
        val p = initial?.clone() ?: PropertyValueMap()
        p.apply(build)
        return p
    }

    /**
     * Don't contain neither line nor column number
     */
    private fun parametersForFillErrorContextAddingMetadata(): List<PropertyValueMap> {
        return listOf(buildErrorContext { this[Property.TOKEN_STRING] = "any" }, buildErrorContext {})
    }

    @Test
    @Parameters
    fun fillErrorContextAddingMetadata(errorContext: PropertyValueMap) {
        val metadata = NodeMetadata(1, 2)
        val expected = buildErrorContext(initial = errorContext) {
            this[Property.LINE_NUMBER] = metadata.line
            this[Property.COLUMN_NUMBER] = metadata.column
        }

        val actual = metadata.fillErrorContext(errorContext)

        assertTrue(actual === errorContext)
        assertEqualsErrorContext(expected, actual)
    }

    /**
     * contain either line, column number or both
     */
    private fun parametersForFillErrorContextNotAddingMetadata(): List<PropertyValueMap> {
        return listOf(buildErrorContext { this[Property.COLUMN_NUMBER] = 1L },
                      buildErrorContext { this[Property.LINE_NUMBER] = 2L },
                      buildErrorContext {
                          this[Property.LINE_NUMBER] = 3L
                          this[Property.COLUMN_NUMBER] = 4L
                      })
    }

    @Test
    @Parameters
    fun fillErrorContextNotAddingMetadata(errorContext: PropertyValueMap) {
        val metadata = NodeMetadata(1, 2)

        // clone to preserve original elements
        val expected = errorContext.clone()

        val actual = metadata.fillErrorContext(errorContext)

        assertTrue(actual === errorContext)
        assertEqualsErrorContext(expected, actual)
    }
}