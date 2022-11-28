/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
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

package org.partiql.examples

import org.partiql.examples.util.Example
import java.io.PrintStream

class EvaluationJavaExampleTest : BaseExampleTest() {
    override fun example(out: PrintStream): Example = EvaluationJavaExample(out)

    override val expected = """
        |PartiQL query:
        |    'Hello, ' || user_name
        |global variables:
        |    user_name => 'Homer Simpson'
        |result
        |    'Hello, Homer Simpson'
        |
    """.trimMargin()
}
