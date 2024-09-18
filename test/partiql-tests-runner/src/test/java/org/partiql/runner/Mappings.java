package org.partiql.runner;

import com.amazon.ion.IonStruct;
import com.amazon.ion.IonSystem;
import com.amazon.ion.IonValue;
import org.partiql_v0_14_8.lang.eval.ExprValue;
import org.partiql_v0_14_8.lang.eval.ExprValueExtensionsKt;
import org.partiql_v0_14_8.lang.util.IonValueExtensionsKt;

/**
 * When using the shadow plugin to relocate APIs, you can't invoke Kotlin-specific features any more. Top-level
 * functions, top-level methods, etc cannot be referenced from Kotlin. This Java class exposes
 * several static methods to allow for the invocation of the shadowed APIs from Kotlin.
 */
public class Mappings {
    public static IonValue toIonValue(ExprValue exprValue, IonSystem system) {
        return ExprValueExtensionsKt.toIonValue(exprValue, system);
    }

    public static String MISSING_ANNOTATION = ExprValueExtensionsKt.MISSING_ANNOTATION;

    public static String BAG_ANNOTATION = ExprValueExtensionsKt.BAG_ANNOTATION;

    public static IonStruct asIonStruct(IonValue value) {
        return IonValueExtensionsKt.asIonStruct(value);
    }

    public static String stringValue(IonValue value) {
        return IonValueExtensionsKt.stringValue(value);
    }
}
