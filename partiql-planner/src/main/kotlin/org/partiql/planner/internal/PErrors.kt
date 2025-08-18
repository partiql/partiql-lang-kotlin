package org.partiql.planner.internal

import org.partiql.spi.SourceLocation
import org.partiql.spi.catalog.Identifier
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorKind
import org.partiql.spi.errors.PRuntimeException
import org.partiql.spi.errors.Severity
import org.partiql.spi.function.FnOverload
import org.partiql.spi.types.PType

internal object PErrors {

    /**
     * @param location see [PError.location]
     * @return an error representing [PError.ALWAYS_MISSING]
     */
    internal fun alwaysMissing(
        location: SourceLocation?
    ): PError {
        return PError(
            PError.ALWAYS_MISSING,
            Severity.WARNING(),
            PErrorKind.SEMANTIC(),
            location,
            null
        )
    }

    /**
     * @param location see [PError.location]
     * @param inputType  see [PError.UNDEFINED_CAST]
     * @param targetType  see [PError.UNDEFINED_CAST]
     * @return an error representing [PError.UNDEFINED_CAST]
     */
    internal fun castUndefined(
        location: SourceLocation?,
        inputType: PType?,
        targetType: PType?
    ): PError {
        return PError(
            PError.UNDEFINED_CAST,
            Severity.WARNING(),
            PErrorKind.SEMANTIC(),
            location,
            mapOf(
                "INPUT_TYPE" to inputType,
                "TARGET_TYPE" to targetType
            )
        )
    }

    /**
     * @param location see [PError.location]
     * @param fnId see [PError.FUNCTION_NOT_FOUND]
     * @param argTypes see [PError.FUNCTION_NOT_FOUND]
     * @return an error representing [PError.FUNCTION_NOT_FOUND]
     */
    internal fun functionNotFound(
        location: SourceLocation?,
        fnId: Identifier?,
        argTypes: List<PType?>?
    ): PError {
        return PError(
            PError.FUNCTION_NOT_FOUND,
            Severity.ERROR(),
            PErrorKind.SEMANTIC(),
            location,
            mapOf(
                "FN_ID" to fnId,
                "ARG_TYPES" to argTypes
            )
        )
    }

    /**
     * @param location see [PError.location]
     * @param fnId see [PError.FUNCTION_TYPE_MISMATCH]
     * @param argTypes see [PError.FUNCTION_TYPE_MISMATCH]
     * @return an error representing [PError.FUNCTION_TYPE_MISMATCH]
     */
    internal fun functionTypeMismatch(
        location: SourceLocation?,
        fnId: Identifier?,
        argTypes: List<PType?>?,
        candidates: List<FnOverload?>?
    ): PError {
        return PError(
            PError.FUNCTION_TYPE_MISMATCH,
            Severity.WARNING(),
            PErrorKind.SEMANTIC(),
            location,
            mapOf(
                "FN_ID" to fnId,
                "ARG_TYPES" to argTypes,
                "CANDIDATES" to candidates
            )
        )
    }

    /**
     * @param location see [PError.location]
     * @return an error representing [PError.PATH_INDEX_NEVER_SUCCEEDS]
     */
    internal fun pathIndexNeverSucceeds(
        location: SourceLocation?
    ): PError {
        return PError(
            PError.PATH_INDEX_NEVER_SUCCEEDS,
            Severity.WARNING(),
            PErrorKind.SEMANTIC(),
            location,
            null
        )
    }

    /**
     * @param location see [PError.location]
     * @return an error representing [PError.PATH_KEY_NEVER_SUCCEEDS]
     */
    internal fun pathKeyNeverSucceeds(
        location: SourceLocation?
    ): PError {
        return PError(
            PError.PATH_KEY_NEVER_SUCCEEDS,
            Severity.WARNING(),
            PErrorKind.SEMANTIC(),
            location,
            null
        )
    }

    /**
     * @param location see [PError.location]
     * @return an error representing [PError.PATH_SYMBOL_NEVER_SUCCEEDS]
     */
    internal fun pathSymbolNeverSucceeds(
        location: SourceLocation?
    ): PError {
        return PError(
            PError.PATH_SYMBOL_NEVER_SUCCEEDS,
            Severity.WARNING(),
            PErrorKind.SEMANTIC(),
            location,
            null
        )
    }

    /**
     * @param location see [PError.location]
     * @param actualType see [PError.TYPE_UNEXPECTED]
     * @param expectedTypes see [PError.TYPE_UNEXPECTED]
     * @return an error representing [PError.TYPE_UNEXPECTED]
     */
    internal fun typeUnexpected(
        location: SourceLocation?,
        actualType: PType?,
        expectedTypes: List<PType?>?
    ): PError {
        return PError(
            PError.TYPE_UNEXPECTED,
            Severity.WARNING(),
            PErrorKind.SEMANTIC(),
            location,
            mapOf("ACTUAL_TYPE" to actualType, "EXPECTED_TYPES" to expectedTypes)
        )
    }

    /**
     * @param location see [PError.location]
     * @param id see [PError.VAR_REF_NOT_FOUND]
     * @param locals see [PError.VAR_REF_NOT_FOUND]
     * @return an error representing [PError.VAR_REF_NOT_FOUND]
     */
    internal fun varRefNotFound(
        location: SourceLocation?,
        id: Identifier?,
        locals: List<String?>?
    ): PError {
        return PError(
            PError.VAR_REF_NOT_FOUND,
            Severity.WARNING(),
            PErrorKind.SEMANTIC(),
            location,
            mapOf("ID" to id, "LOCALS" to locals)
        )
    }

    /**
     * @param path see [PError.INVALID_EXCLUDE_PATH]
     * @return an error representing [PError.INVALID_EXCLUDE_PATH]
     */
    internal fun invalidExcludePath(
        path: String
    ): PError {
        return PError(
            PError.INVALID_EXCLUDE_PATH,
            Severity.WARNING(),
            PErrorKind.SEMANTIC(),
            null,
            mapOf("PATH" to path)
        )
    }

    internal fun internalErrorException(cause: Throwable): PRuntimeException {
        return PRuntimeException(internalError(cause))
    }

    internal fun featureNotSupported(feature: String, location: SourceLocation? = null): PError {
        return PError(
            PError.FEATURE_NOT_SUPPORTED,
            Severity.ERROR(),
            PErrorKind.SEMANTIC(),
            location,
            mapOf("FEATURE_NAME" to feature),
        )
    }

    internal fun degreeViolationScalarSubquery(actual: Int, location: SourceLocation? = null): PError {
        return PError(
            PError.DEGREE_VIOLATION_SCALAR_SUBQUERY,
            Severity.ERROR(),
            PErrorKind.SEMANTIC(),
            location,
            mapOf("ACTUAL" to actual),
        )
    }

    internal fun internalError(cause: Throwable): PError = PError(
        PError.INTERNAL_ERROR,
        Severity.ERROR(),
        PErrorKind.SEMANTIC(),
        null,
        mapOf("CAUSE" to cause),
    )
}
