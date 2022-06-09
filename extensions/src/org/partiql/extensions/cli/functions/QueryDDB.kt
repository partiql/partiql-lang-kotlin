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
package org.partiql.extensions.cli.functions

import com.amazon.ion.system.IonReaderBuilder
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.document.ItemUtils
import com.amazonaws.services.dynamodbv2.model.ExecuteStatementRequest
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.stringValue
import org.partiql.lang.types.FunctionSignature
import org.partiql.lang.types.StaticType

/**
 * [QueryDDB] is a function to perform a single query on DDB using PartiQL.
 * It uses the default configuration provider to establish a connection with AWS. Please reference the official
 * AWS documentation for specifying which account/profile to use via credentials overrides. Reference the CLI.md
 * file within this repository for more information.
 */
class QueryDDB(valueFactory: ExprValueFactory) : BaseFunction(valueFactory) {

    private val client: AmazonDynamoDB = AmazonDynamoDBClientBuilder.defaultClient()

    override val signature = FunctionSignature(
        name = "query_ddb",
        requiredParameters = listOf(StaticType.STRING),
        optionalParameter = StaticType.NULL_OR_MISSING,
        returnType = StaticType.LIST
    )

    override fun callWithRequired(session: EvaluationSession, required: List<ExprValue>): ExprValue {
        val queryString = required[0].stringValue()
        val request = ExecuteStatementRequest().withStatement(queryString)
        var nextToken: String?
        val items = mutableListOf<String>()
        do {
            val result = client.executeStatement(request)
            result.items.forEach { items.add(ItemUtils.toItem(it).toJSON()) }
            nextToken = result.nextToken
            request.withNextToken(nextToken)
        } while (nextToken != null)
        return IonReaderBuilder.standard().build(items.joinToString(separator = "")).use { reader ->
            val ionValues = mutableListOf<ExprValue>()
            var type = reader.next()
            while (type != null) {
                ionValues.add(valueFactory.newFromIonReader(reader))
                type = reader.next()
            }
            valueFactory.newList(ionValues)
        }
    }
}
