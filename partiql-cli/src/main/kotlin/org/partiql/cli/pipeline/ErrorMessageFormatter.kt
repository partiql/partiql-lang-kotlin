package org.partiql.cli.pipeline

import org.partiql.cli.ErrorCodeString
import org.partiql.spi.catalog.Identifier
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorKind
import org.partiql.spi.errors.Severity
import org.partiql.spi.function.FnOverload
import org.partiql.spi.types.PType
import java.io.PrintWriter
import java.io.Writer

object ErrorMessageFormatter {
    private val errorCodeStringValues = ErrorCodeString.entries.toTypedArray()
    fun message(error: PError): String {
        val errorCode = errorCodeStringValues.find { it.code == error.code() }
        val content = when (errorCode) {
            ErrorCodeString.UNEXPECTED_TOKEN -> unexpectedToken(error)
            ErrorCodeString.UNRECOGNIZED_TOKEN -> unrecognizedToken(error)
            ErrorCodeString.FUNCTION_TYPE_MISMATCH -> fnTypeMismatch(error)
            ErrorCodeString.FUNCTION_NOT_FOUND -> fnTypeMismatch(error) // TODO: Add dedicated message for this.
            ErrorCodeString.UNDEFINED_CAST -> undefinedCast(error)
            ErrorCodeString.INTERNAL_ERROR -> internalError(error) // TODO: Make the verbosity a variable
            ErrorCodeString.PATH_KEY_NEVER_SUCCEEDS -> pathNeverSucceeds("key")
            ErrorCodeString.PATH_SYMBOL_NEVER_SUCCEEDS -> pathNeverSucceeds("symbol")
            ErrorCodeString.PATH_INDEX_NEVER_SUCCEEDS -> pathNeverSucceeds("index")
            ErrorCodeString.ALWAYS_MISSING -> alwaysMissing()
            ErrorCodeString.FEATURE_NOT_SUPPORTED -> featureNotSupported(error)
            ErrorCodeString.VAR_REF_AMBIGUOUS -> varRefAmbiguous(error)
            ErrorCodeString.VAR_REF_NOT_FOUND -> varRefNotFound(error)
            ErrorCodeString.INVALID_EXCLUDE_PATH -> invalidExcludePath(error)
            ErrorCodeString.CARDINALITY_VIOLATION -> cardinalityViolation()
            ErrorCodeString.NUMERIC_VALUE_OUT_OF_RANGE -> numericValueOutOfRange(error)
            ErrorCodeString.INVALID_CHAR_VALUE_FOR_CAST -> invalidCharValueForCast(error)
            ErrorCodeString.DIVISION_BY_ZERO -> divisionByZero(error)
            ErrorCodeString.TYPE_UNEXPECTED -> typeUnexpected(error)
            ErrorCodeString.ALL -> "INTERNAL ERROR: This should never have occurred."
            null -> "Unrecognized error code received: ${error.code()}"
        }
        return buildString {
            val type = when (error.severity.code()) {
                Severity.ERROR -> "e"
                Severity.WARNING -> "w"
                else -> "UNKNOWN"
            }
            val classification = when (error.kind.code()) {
                PErrorKind.SYNTAX -> "syntax"
                PErrorKind.SEMANTIC -> "semantic"
                PErrorKind.COMPILATION -> "compile"
                PErrorKind.EXECUTION -> "runtime"
                else -> "unknown"
            }
            val loc = error.location
            val location = getLocationString(loc?.line, loc?.offset, loc?.length)
            append(prepare(type, postfix = ":"))
            append(prepare(classification, " [", "]"))
            append(prepare(location, " "))
            append(prepare(content, " "))
        }
    }

    /**
     * @see PError.ALWAYS_MISSING
     */
    private fun alwaysMissing(): String {
        return "Expression always returns missing"
    }

    /**
     * @see PError.VAR_REF_NOT_FOUND
     */
    private fun varRefNotFound(error: PError): String {
        val id = error.getOrNull("ID", Identifier::class.java)
        val idStr = prepare(id.toString(), " (", ")")
        val locals = error.getListOrNull("LOCALS", String::class.java)
        val localsStr = when (locals.isNullOrEmpty()) {
            true -> ""
            false -> " Locals: $locals."
        }
        return "Variable reference$idStr could not be found in the database environment or in the set of available locals.$localsStr"
    }

    /**
     * @see PError.INVALID_EXCLUDE_PATH
     */
    private fun invalidExcludePath(error: PError): String {
        val path = error.getOrNull("PATH", String::class.java)
        val pathStr = prepare(path, " (", ")")
        return "Invalid exclude path$pathStr."
    }

    /**
     * @see PError.CARDINALITY_VIOLATION
     */
    private fun cardinalityViolation(): String {
        return "Cardinality violation."
    }

    /**
     * @see PError.NUMERIC_VALUE_OUT_OF_RANGE
     */
    private fun numericValueOutOfRange(error: PError): String {
        val value = error.getOrNull("VALUE", String::class.java)
        val valueStr = prepare(value.toString(), " (", ")")
        val type = error.getOrNull("TYPE", PType::class.java)
        val typeString = prepare(type.toString(), " for type ")
        return "Numeric value$valueStr is out of range$typeString."
    }

    /**
     * @see PError.INVALID_CHAR_VALUE_FOR_CAST
     */
    private fun invalidCharValueForCast(error: PError): String {
        val value = error.getOrNull("VALUE", String::class.java)
        val valueStr = prepare(value.toString(), " (", ")")
        val type = error.getOrNull("TYPE", PType::class.java)
        val typeString = prepare(type.toString(), " to type ")
        return "Invalid character value$valueStr for cast$typeString."
    }

    /**
     * @see PError.DIVISION_BY_ZERO
     */
    private fun divisionByZero(error: PError): String {
        val dividendType = error.getOrNull("DIVIDEND_TYPE", PType::class.java)
        val dividendTypeStr = prepare(dividendType.toString(), " of type ")
        val dividend = error.getOrNull("DIVIDEND", String::class.java)
        val dividendStr = prepare(dividend.toString(), " ")
        return "Cannot divide$dividendStr$dividendTypeStr by zero."
    }

    /**
     * @see PError.TYPE_UNEXPECTED
     */
    private fun typeUnexpected(error: PError): String {
        val expectedTypes = error.getListOrNull("EXPECTED_TYPES", PType::class.java)
        val actualType = error.getOrNull("ACTUAL_TYPE", PType::class.java)
        val expectedTypesStr = when (expectedTypes.isNullOrEmpty()) {
            true -> ""
            false -> " Expected types: $expectedTypes."
        }
        val actualTypeStr = prepare(actualType.toString(), " Received: ", ".")
        return "Type mismatch.$expectedTypesStr$actualTypeStr"
    }

    /**
     * @see PError.VAR_REF_AMBIGUOUS
     */
    private fun varRefAmbiguous(error: PError): String {
        val id = error.getOrNull("ID", Identifier::class.java)
        val idStr = prepare(id.toString(), " (", ")")
        return "Variable reference$idStr is ambiguous."
    }

    private fun featureNotSupported(error: PError): String {
        val name = error.getOrNull("FEATURE_NAME", String::class.java)
        val nameStr = prepare(name, " (", ")")
        return "Feature$nameStr not supported"
    }

    private fun pathNeverSucceeds(type: String): String {
        return "Path-$type expression always returns missing."
    }

    private fun internalError(error: PError): String {
        val cause = error.getOrNull("CAUSE", Throwable::class.java)
        val writer = StringPrintWriter()
        writer.appendLine("Unexpected failure encountered. Caused by: $cause.")
        cause.printStackTrace(writer)
        return writer.w.sb.toString()
    }

    /**
     * @see PError.UNDEFINED_CAST
     */
    private fun undefinedCast(error: PError): String {
        val castFrom = error.getOrNull("INPUT_TYPE", PType::class.java)
        val castTo = error.getOrNull("TARGET_TYPE", PType::class.java)
        val castFromStr = prepare(castFrom.toString(), " from ", "")
        val castToStr = prepare(castTo.toString(), " to ", "")
        return "Undefined cast$castFromStr$castToStr."
    }

    /**
     * @see PError.FUNCTION_TYPE_MISMATCH
     */
    private fun fnTypeMismatch(error: PError): String {
        val functionName = error.getOrNull("FN_ID", Identifier::class.java)
        val candidates = error.getListOrNull("CANDIDATES", FnOverload::class.java)
        val args = error.getListOrNull("ARG_TYPES", PType::class.java)
        val fnNameStr = prepare(functionName.toString(), " ", "")
        val fnStr = when {
            functionName != null && args != null -> fnNameStr + args.joinToString(", ", "(", ")") { it.toString() }
            functionName != null && args == null -> fnNameStr
            else -> ""
        }
        return buildString {
            append("Undefined function$fnStr.")
        }
    }

    private fun unrecognizedToken(error: PError): String {
        val token = error.getOrNull("CONTENT", String::class.java)
        val tokenStr = prepare(token, " (", ")")
        return "Unrecognized token$tokenStr."
    }

    /**
     * @see PError.UNEXPECTED_TOKEN
     */
    private fun unexpectedToken(error: PError): String {
        val tokenName = error.getOrNull("TOKEN_NAME", String::class.java)
        val token = error.getOrNull("CONTENT", String::class.java)
        val expected = error.getListOrNull("EXPECTED_TOKENS", String::class.java)
        val tokenNameStr = prepare(tokenName, " (", ")")
        val tokenStr = prepare(token, " \"", "\"")
        val expectedStr = when (expected.isNullOrEmpty()) {
            true -> ""
            false -> prepare("Expected one of: $expected", " ")
        }
        return "Unexpected token$tokenNameStr$tokenStr.$expectedStr"
    }

    private fun prepare(str: String?, prefix: String = "", postfix: String = ""): String {
        return when (str) {
            null -> ""
            else -> buildString {
                append(prefix)
                append(str)
                append(postfix)
            }
        }
    }

    private fun getLocationString(line: Long?, column: Long?, length: Long?): String? {
        return when {
            line == null || column == null -> null
            length == null -> "$line:$column"
            else -> "$line:$column:$length"
        }
    }

    private class StringPrintWriter private constructor(
        val w: StringBuilderWriter
    ) : PrintWriter(w) {
        constructor() : this(StringBuilderWriter())
    }

    private class StringBuilderWriter : Writer() {
        val sb = StringBuilder()
        override fun close() {}

        override fun flush() {}

        override fun write(cbuf: CharArray, off: Int, len: Int) {
            val end = off + (len - 1)
            sb.append(cbuf.sliceArray(off..end))
        }
    }
}
