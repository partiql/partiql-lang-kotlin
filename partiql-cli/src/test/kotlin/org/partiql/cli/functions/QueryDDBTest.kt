package org.partiql.cli.functions

import com.amazon.ion.system.IonSystemBuilder
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.ExecuteStatementResult
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.junit.jupiter.MockitoExtension
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.toIonValue

@ExtendWith(MockitoExtension::class)
class QueryDDBTest {

    private val ion = IonSystemBuilder.standard().build()
    private val session = EvaluationSession.standard()
    private val client: AmazonDynamoDBClient = Mockito.mock(AmazonDynamoDBClient::class.java)
    private lateinit var function: QueryDDB

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        this.function = QueryDDB(ion, client)
    }

    @Test
    fun basicQuery() {
        // Arrange
        val arguments = listOf(ExprValue.newString("SELECT * FROM test;"))
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
        val arguments = listOf(ExprValue.newString("SELECT * FROM test;"))
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
        Assertions.assertEquals(ion.loader.load(expected), ion.loader.load(actual))
    }
}
