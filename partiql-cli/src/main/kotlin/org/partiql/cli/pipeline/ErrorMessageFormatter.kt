package org.partiql.cli.pipeline

import org.partiql.cli.ErrorCodeString
import org.partiql.spi.catalog.Identifier
import org.partiql.spi.errors.Error
import org.partiql.spi.function.Function
import org.partiql.types.PType
import java.io.PrintWriter
import java.io.Writer

object ErrorMessageFormatter {
    private val errorCodeStringValues = ErrorCodeString.entries.toTypedArray()
    fun message(error: Error): String {
        val errorCode = errorCodeStringValues.find { it.code == error.code }
        val content = when (errorCode) {
            ErrorCodeString.UNEXPECTED_TOKEN -> unexpectedToken(error)
            ErrorCodeString.UNRECOGNIZED_TOKEN -> unrecognizedToken(error)
            ErrorCodeString.FUNCTION_TYPE_MISMATCH -> fnTypeMismatch(error)
            ErrorCodeString.FUNCTION_NOT_FOUND -> fnTypeMismatch(error) // TODO: Add dedicated message for this.
            ErrorCodeString.UNDEFINED_CAST -> undefinedCast(error)
            ErrorCodeString.INTERNAL_ERROR -> internalError(error, true) // TODO: Make the verbosity a variable
            ErrorCodeString.PATH_KEY_NEVER_SUCCEEDS -> pathNeverSucceeds("key")
            ErrorCodeString.PATH_SYMBOL_NEVER_SUCCEEDS -> pathNeverSucceeds("symbol")
            ErrorCodeString.PATH_INDEX_NEVER_SUCCEEDS -> pathNeverSucceeds("index")
            ErrorCodeString.ALWAYS_MISSING -> alwaysMissing()
            ErrorCodeString.FEATURE_NOT_SUPPORTED -> featureNotSupported(error)
            ErrorCodeString.VAR_REF_AMBIGUOUS -> varRefAmbiguous(error)
            ErrorCodeString.VAR_REF_NOT_FOUND -> varRefNotFound(error)
            ErrorCodeString.ALL -> "INTERNAL ERROR: This should never have occurred."
            ErrorCodeString.UNKNOWN, null -> "Unrecognized error code received: ${error.code}"
        }
        return buildString {
            val loc = error.location
            val location = getLocationString(loc?.line, loc?.offset, loc?.length)
            append(location)
            append(content)
        }
    }

    /**
     * @see Error.ALWAYS_MISSING
     */
    private fun alwaysMissing(): String {
        return "Expression always returns missing"
    }

    /**
     * @see Error.VAR_REF_NOT_FOUND
     */
    private fun varRefNotFound(error: Error): String {
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
     * @see Error.VAR_REF_AMBIGUOUS
     */
    private fun varRefAmbiguous(error: Error): String {
        val id = error.getOrNull("ID", Identifier::class.java)
        val idStr = prepare(id.toString(), " (", ")")
        return "Variable reference$idStr is ambiguous."
    }

    private fun featureNotSupported(error: Error): String {
        val name = error.getOrNull("FEATURE_NAME", String::class.java)
        val nameStr = prepare(name, " (", ")")
        return "Feature$nameStr not supported"
    }

    private fun pathNeverSucceeds(type: String): String {
        return "Path-$type expression always returns missing."
    }

    private fun internalError(error: Error, _verbose: Boolean): String {
        return when (_verbose) {
            true -> {
                val cause = error.getOrNull("CAUSE", Throwable::class.java)
                val writer = StringPrintWriter()
                writer.appendLine("Unexpected failure encountered. Caused by: $cause.")
                cause.printStackTrace(writer)
                writer.w.sb.toString()
            }
            false -> "Unexpected failure encountered. Run with --verbose for more information."
        }
    }

    /**
     * @see Error.UNDEFINED_CAST
     */
    private fun undefinedCast(error: Error): String {
        val castFrom = error.getOrNull("INPUT_TYPE", PType::class.java)
        val castTo = error.getOrNull("TARGET_TYPE", PType::class.java)
        val castFromStr = prepare(castFrom.toString(), " from ", "")
        val castToStr = prepare(castTo.toString(), " to ", "")
        return "Undefined cast$castFromStr$castToStr."
    }

    /**
     * @see Error.FUNCTION_TYPE_MISMATCH
     */
    private fun fnTypeMismatch(error: Error): String {
        val functionName = error.getOrNull("FN_ID", Identifier::class.java)
        val candidates = error.getListOrNull("CANDIDATES", Function::class.java)
        val args = error.getListOrNull("ARG_TYPES", PType::class.java)
        val fnNameStr = prepare(functionName.toString(), " ", "")
        val fnStr = when {
            functionName != null && args != null -> fnNameStr + args.joinToString(", ", "(", ")") { it.toString() }
            functionName != null && args == null -> fnNameStr
            else -> ""
        }
        return buildString {
            append("Undefined function$fnStr.")
            if (!candidates.isNullOrEmpty()) {
                appendLine(" Did you mean: ")
                for (variant in candidates) {
                    variant as Function
                    append("- ")
                    append(variant.getName())
                    append(
                        variant.getParameters().joinToString(", ", "(", ")") {
                            "${it.getName()}: ${it.getType()}"
                        }
                    )
                    appendLine()
                }
            }
        }
    }

    private fun unrecognizedToken(error: Error): String {
        val token = error.getOrNull("CONTENT", String::class.java)
        val tokenStr = prepare(token, " (", ")")
        return "Unrecognized token$tokenStr."
    }

    /**
     * @see Error.UNEXPECTED_TOKEN
     */
    private fun unexpectedToken(error: Error): String {
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

    private fun getLocationString(line: Long?, column: Long?, length: Long?): String {
        return when {
            line == null || column == null -> ""
            length == null -> "$line:$column "
            else -> "$line:$column:$length "
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
            val end = off + len
            sb.append(cbuf.sliceArray(off..end))
        }
    }
}
