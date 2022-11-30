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

import com.amazon.ion.system.IonSystemBuilder
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.ExecuteStatementResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.junit.jupiter.MockitoExtension
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.toIonValue

@ExtendWith(MockitoExtension::class)
class QueryDDBTest {

    private val ion = IonSystemBuilder.standard().build()
    private val factory = ExprValueFactory.standard(ion)
    private val session = EvaluationSession.standard()
    private val client: AmazonDynamoDBClient = Mockito.mock(AmazonDynamoDBClient::class.java)
    private lateinit var function: QueryDDB

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        this.function = QueryDDB(factory, client)
    }

    @Test
    fun basicQuery() {
        // Arrange
        val arguments = listOf(factory.newString("SELECT * FROM test;"))
        val mockAttrValue = AttributeValue()
        mockAttrValue.s = "value"
        val mockResults = listOf(mapOf("key" to mockAttrValue))
        val mockResult = ExecuteStatementResult().withItems(mockResults)
        Mockito.doReturn(mockResult).`when`(client).executeStatement(ArgumentMatchers.any())
        val expected = "[{key: \"value\"}]"

        // Act
        val result = function.callWithRequired(session, arguments)

        // Assert
        assertAsIon(expected, result.toIonValue(ion).toString())
    }

    @Test
    fun basicQueryWithNextToken() {
        // Arrange
        val arguments = listOf(factory.newString("SELECT * FROM test;"))
        val mockAttrValue = AttributeValue()
        mockAttrValue.s = "value"
        val mockResults = listOf(mapOf("key" to mockAttrValue))
        val mockResult1 = ExecuteStatementResult().withItems(mockResults).withNextToken("1")
        val mockResult2 = ExecuteStatementResult().withItems(mockResults)
        Mockito.`when`(client.executeStatement(ArgumentMatchers.any())).thenReturn(mockResult1).thenReturn(mockResult2)
        val expected = "[{key: \"value\"}, {key: \"value\"}]"

        // Act
        val result = function.callWithRequired(session, arguments)

        // Assert
        assertAsIon(expected, result.toIonValue(ion).toString())
    }

    private fun assertAsIon(expected: String, actual: String) {
        assertEquals(ion.loader.load(expected), ion.loader.load(actual))
    }
}
