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

package org.partiql.testframework.testcar

import com.amazon.ion.*
import org.partiql.lang.*
import org.partiql.lang.eval.*
import org.partiql.testframework.contracts.*
import org.partiql.testframework.util.*

class TestExecutor(private val valueFactory: ExprValueFactory) {
    fun execute(sql: String,
                environmentStruct: IonStruct,
                sessionStruct: IonStruct,
                compileOptionsStruct: IonStruct): CarResponse = try {

        val evalData = EvaluationData(valueFactory, environmentStruct, sessionStruct, compileOptionsStruct)

        val pipeline = CompilerPipeline.build(valueFactory) {
            compileOptions(evalData.compileOptions)
        }

        val compiledQuery = pipeline.compile(sql)

        val result = compiledQuery.eval(evalData.evaluationSession)

        ExecuteSuccess(serializeExprValue(result))
    }
    catch (ex: SqlException) {
        ExecuteFailed(ex.message, ex.errorCode.toString(), ex.errorContext?.toStruct(valueFactory.ion))
    }
    catch (ex: Exception) {
        Error(ex.message!!, ex.getStackTraceString())
    }
}
