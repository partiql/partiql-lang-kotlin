/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *       http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.tests.runner

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import java.util.stream.Stream

/**
 * Reduces some of the boilerplate associated with the style of parameterized testing frequently
 * utilized in this package.
 *
 * Since JUnit5 requires `@JvmStatic` on its `@MethodSource` argument factory methods, this requires all
 * of the argument lists to reside in the companion object of a test class.  This can be annoying since it
 * forces the test to be separated from its tests cases.
 *
 * Classes that derive from this class can be defined near the `@ParameterizedTest` functions instead.
 */
abstract class ArgumentsProviderBase : ArgumentsProvider {

    abstract fun getParameters(): List<Any>

    @Throws(Exception::class)
    override fun provideArguments(extensionContext: ExtensionContext): Stream<out Arguments>? {
        return getParameters().map { Arguments.of(it) }.stream()
    }
}
