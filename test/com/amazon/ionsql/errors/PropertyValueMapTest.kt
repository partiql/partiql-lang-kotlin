package com.amazon.ionsql.errors

import com.amazon.ionsql.*
import com.amazon.ionsql.errors.Property.*
import com.amazon.ionsql.syntax.TokenType
import org.junit.Before
import org.junit.Test


class PropertyValueMapTest : Base() {

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