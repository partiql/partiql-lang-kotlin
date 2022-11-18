/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */
package org.partiql.lang.eval

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.util.ArgumentsProviderBase

class EvaluatingCompilerCastTest : CastTestBase() {
    class ConfiguredCastArguments : ArgumentsProviderBase() {
        override fun getParameters() = allConfiguredTestCases
    }

    class DateTimeConfiguredCastArguments : ArgumentsProviderBase() {
        override fun getParameters() = allConfiguredDateTimeTestCases
    }

    @ParameterizedTest
    @ArgumentsSource(ConfiguredCastArguments::class)
    fun configuredCast(configuredCastCase: ConfiguredCastCase) = configuredCastCase.assertCase()

    @ParameterizedTest
    @ArgumentsSource(DateTimeConfiguredCastArguments::class)
    fun dateTimeConfiguredCast(dateTimeConfiguredCastCase: ConfiguredCastCase) =
        dateTimeConfiguredCastCase.assertCase()
}
