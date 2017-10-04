package com.amazon.ionsql.eval

import com.amazon.ionsql.errors.*
import com.amazon.ionsql.util.*
import junitparams.*
import org.assertj.core.api.Assertions.*
import org.junit.*
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