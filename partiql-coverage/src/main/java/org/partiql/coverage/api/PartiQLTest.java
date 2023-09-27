/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.partiql.coverage.api;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.partiql.coverage.api.impl.PartiQLTestExtension;

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks test methods as PartiQL Tests. Must use JUnit5 to run tests.<br><br>
 * <p>
 * The test method must have two parameters:<br>
 *
 * <pre>1. The PartiQLTestCase representing a test case provided by the PartiQLTestProvider</pre>
 * <pre>2. The PartiQLResult (for performing assertions).</pre>
 * <br>
 * <p>
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
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@TestTemplate
@ExtendWith(PartiQLTestExtension.class)
public @interface PartiQLTest {
    // NOTE: This is intentionally a Java annotation interface as we need to attach @TestTemplate.

    /**
     * @return a Java Class implementing PartiQLTestProvider. This must expose a no-argument constructor.
     */
    Class<? extends PartiQLTestProvider> provider();
}
