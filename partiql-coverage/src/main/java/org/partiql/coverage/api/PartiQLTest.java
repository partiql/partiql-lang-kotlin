package org.partiql.coverage.api;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.partiql.coverage.api.impl.*;

import java.lang.annotation.*;

/**
 * Marks test methods as PartiQL Tests. Must use JUnit5 to run tests.<br><br>
 *
 * The test method must have two parameters:<br>
 * 
 * <pre>1. The PartiQLTestCase representing a test case provided by the PartiQLTestProvider</pre>
 * <pre>2. The PartiQLResult containing the ExprValue.</pre>
 * <br>
 *
 * Example Kotlin Usage:<br>
 *
 * <pre>
 * <code>&#64;PartiQLTest(Provider::class)
 * public fun myTest(tc: CustomPartiQLTestCase, result: PartiQLResult) {
 *     assert(result.value == PartiQLInt(2))
 * }
 * </code>
 * </pre>
 */
@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@TestTemplate
@ExtendWith(PartiQLTestExtension.class)
public @interface PartiQLTest {

    /**
     * @return a Java Class implementing PartiQLTestProvider. This must expose a no-argument constructor.
     */
    Class<? extends PartiQLTestProvider> provider();
}