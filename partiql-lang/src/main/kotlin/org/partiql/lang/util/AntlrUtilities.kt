/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.partiql.lang.util

import com.amazon.ion.Decimal
import com.amazon.ion.IonValue
import com.amazon.ion.system.IonSystemBuilder
import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.ionBool
import com.amazon.ionelement.api.ionDecimal
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionNull
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionSymbol
import com.amazon.ionelement.api.loadSingleElement
import com.amazon.ionelement.api.toIonValue
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.tree.TerminalNode
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.errors.PropertyValueMap
import org.partiql.lang.syntax.ParserException
import org.partiql.parser.antlr.PartiQLParser
import java.math.BigInteger

// workaround until ErrorAndErrorContexts no longer uses IonSystem
private val ion = IonSystemBuilder.standard().build()

internal fun TerminalNode?.error(
    message: String,
    errorCode: ErrorCode,
    errorContext: PropertyValueMap = PropertyValueMap(),
    cause: Throwable? = null,
) = when (this) {
    null -> ParserException(errorCode = errorCode, errorContext = errorContext, cause = cause)
    else -> this.symbol.error(message, errorCode, errorContext, cause)
}

internal fun Token?.error(
    message: String,
    errorCode: ErrorCode,
    errorContext: PropertyValueMap = PropertyValueMap(),
    cause: Throwable? = null,
) = when (this) {
    null -> ParserException(errorCode = errorCode, errorContext = errorContext, cause = cause)
    else -> {
        errorContext[Property.LINE_NUMBER] = this.line.toLong()
        errorContext[Property.COLUMN_NUMBER] = this.charPositionInLine.toLong() + 1
        errorContext[Property.TOKEN_DESCRIPTION] = this.type.getAntlrDisplayString()
        errorContext[Property.TOKEN_VALUE] = getIonValue(this)
        ParserException(message, errorCode, errorContext, cause)
    }
}

internal fun getIonValue(token: Token): IonValue {
    return getIonElement(token).toIonValue(ion)
}

/**
 * Returns the corresponding [IonElement] for a particular ANTLR Token
 */
internal fun getIonElement(token: Token): IonElement {
    val type = token.type
    val text = token.text
    return when (type) {
        PartiQLParser.EOF -> ionSymbol("EOF")
        PartiQLParser.ION_CLOSURE -> loadSingleElement(text.trimStart('`').trimEnd('`'))
        PartiQLParser.TRUE -> ionBool(true)
        PartiQLParser.FALSE -> ionBool(false)
        PartiQLParser.NULL -> ionNull()
        PartiQLParser.NULLS -> ionSymbol("nulls")
        PartiQLParser.MISSING -> ionNull()
        PartiQLParser.LITERAL_STRING -> ionString(text.trim('\'').replace("''", "'"))
        PartiQLParser.LITERAL_INTEGER -> ionInt(BigInteger(text, 10))
        PartiQLParser.LITERAL_DECIMAL -> try {
            ionDecimal(Decimal.valueOf(text))
        } catch (e: NumberFormatException) {
            throw token.error(e.localizedMessage, ErrorCode.PARSE_EXPECTED_NUMBER, cause = e)
        }
        PartiQLParser.IDENTIFIER_QUOTED -> ionSymbol(text.trim('\"').replace("\"\"", "\""))
        else -> ionSymbol(text)
    }
}

internal fun Int.getAntlrDisplayString(): String = PartiQLParser.VOCABULARY.getSymbolicName(this)
