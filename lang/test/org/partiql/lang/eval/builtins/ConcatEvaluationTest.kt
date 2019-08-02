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

package org.partiql.lang.eval.builtins

import org.junit.*
import com.amazon.ion.*
import org.partiql.lang.*
import org.partiql.lang.errors.*
import org.partiql.lang.eval.*
import org.partiql.lang.util.*

class ConcatEvaluationTest : EvaluatorTestBase() {
    private val env = Environment.standard()

    @Test
    fun concatFailed() = try {
        voidEval("'hi' || 1")
        fail("didn't throw")
    }
    catch (e: EvaluationException) {
        softAssert {
            assertThat(e.message).isEqualTo("Expected text: 1")
            assertThat(e.errorCode).isEqualTo(ErrorCode.EVALUATOR_CONCAT_FAILED_DUE_TO_INCOMPATIBLE_TYPE)
        }
    }
}
