package com.amazon.ionsql.syntax

import com.amazon.ion.*
import com.amazon.ionsql.*
import com.amazon.ionsql.util.*

abstract class IonSqlParserBase : Base() {
    protected val parser = IonSqlParser(ion)

    protected fun parse(source: String): IonSexp = parser.parse(source)

    protected fun assertExpression(expectedText: String, source: String) {
        val actual = parse(source).filterMetaNodes()
        val expected = literal(expectedText)

        assertEquals(expected, actual)
    }
}
