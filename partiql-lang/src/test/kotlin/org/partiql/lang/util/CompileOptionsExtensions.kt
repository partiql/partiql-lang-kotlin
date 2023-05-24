package org.partiql.lang.util

import org.partiql.lang.eval.CompileOptions
import org.partiql.lang.eval.TypedOpBehavior
import org.partiql.lang.eval.TypingMode

fun CompileOptions.Builder.legacyCastBehavior() {
    @Suppress("DEPRECATION") // TypedOpBehavior.LEGACY is deprecated.
    typedOpBehavior(TypedOpBehavior.LEGACY)
}

fun CompileOptions.Builder.honorTypedOpParameters() {
    typedOpBehavior(TypedOpBehavior.HONOR_PARAMETERS)
}

fun CompileOptions.Builder.legacyTypingMode() {
    typingMode(TypingMode.LEGACY)
}

fun CompileOptions.Builder.permissiveTypingMode() {
    typingMode(TypingMode.PERMISSIVE)
}
