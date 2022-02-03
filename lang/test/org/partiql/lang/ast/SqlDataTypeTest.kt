package org.partiql.lang.ast

import org.junit.Assert
import org.junit.Test

class SqlDataTypeTest {
    @Test
    fun `accessing non-companion object should not cause NPE`() {
        // Accessing companion object to see if they are initialized properly.
        Assert.assertTrue(SqlDataType.MISSING != null)
        Assert.assertTrue(SqlDataType.NULL != null)
        Assert.assertTrue(SqlDataType.BOOLEAN != null)
        Assert.assertTrue(SqlDataType.SMALLINT != null)
        Assert.assertTrue(SqlDataType.INTEGER4 != null)
        Assert.assertTrue(SqlDataType.INTEGER != null)
        Assert.assertTrue(SqlDataType.FLOAT != null)
        Assert.assertTrue(SqlDataType.REAL != null)
        Assert.assertTrue(SqlDataType.DOUBLE_PRECISION != null)
        Assert.assertTrue(SqlDataType.DECIMAL != null)
        Assert.assertTrue(SqlDataType.NUMERIC != null)
        Assert.assertTrue(SqlDataType.TIMESTAMP != null)
        Assert.assertTrue(SqlDataType.CHARACTER != null)
        Assert.assertTrue(SqlDataType.CHARACTER_VARYING != null)
        Assert.assertTrue(SqlDataType.STRING != null)
        Assert.assertTrue(SqlDataType.SYMBOL != null)
        Assert.assertTrue(SqlDataType.CLOB != null)
        Assert.assertTrue(SqlDataType.BLOB != null)
        Assert.assertTrue(SqlDataType.STRUCT != null)
        Assert.assertTrue(SqlDataType.TUPLE != null)
        Assert.assertTrue(SqlDataType.LIST != null)
        Assert.assertTrue(SqlDataType.SEXP != null)
        Assert.assertTrue(SqlDataType.BAG != null)
        Assert.assertTrue(SqlDataType.ANY != null)
        Assert.assertTrue(SqlDataType.DATE != null)
        Assert.assertTrue(SqlDataType.TIME != null)
        Assert.assertTrue(SqlDataType.TIME_WITH_TIME_ZONE != null)

        // Accessing companion object to see all values should be initialized.
        SqlDataType.values().forEach {
            Assert.assertTrue(it != null)
        }
    }

    @Test
    fun sqlDataTypeAccessingCompanionTest() {
        // Accessing companion object should not cause any initialization
        // problems.
        SqlDataType.values().forEach {
            Assert.assertTrue(it != null)
        }

        Assert.assertTrue(SqlDataType.MISSING != null)
        Assert.assertTrue(SqlDataType.NULL != null)
        Assert.assertTrue(SqlDataType.BOOLEAN != null)
        Assert.assertTrue(SqlDataType.SMALLINT != null)
        Assert.assertTrue(SqlDataType.INTEGER4 != null)
        Assert.assertTrue(SqlDataType.INTEGER != null)
        Assert.assertTrue(SqlDataType.FLOAT != null)
        Assert.assertTrue(SqlDataType.REAL != null)
        Assert.assertTrue(SqlDataType.DOUBLE_PRECISION != null)
        Assert.assertTrue(SqlDataType.DECIMAL != null)
        Assert.assertTrue(SqlDataType.NUMERIC != null)
        Assert.assertTrue(SqlDataType.TIMESTAMP != null)
        Assert.assertTrue(SqlDataType.CHARACTER != null)
        Assert.assertTrue(SqlDataType.CHARACTER_VARYING != null)
        Assert.assertTrue(SqlDataType.STRING != null)
        Assert.assertTrue(SqlDataType.SYMBOL != null)
        Assert.assertTrue(SqlDataType.CLOB != null)
        Assert.assertTrue(SqlDataType.BLOB != null)
        Assert.assertTrue(SqlDataType.STRUCT != null)
        Assert.assertTrue(SqlDataType.TUPLE != null)
        Assert.assertTrue(SqlDataType.LIST != null)
        Assert.assertTrue(SqlDataType.SEXP != null)
        Assert.assertTrue(SqlDataType.BAG != null)
        Assert.assertTrue(SqlDataType.ANY != null)
        Assert.assertTrue(SqlDataType.DATE != null)
        Assert.assertTrue(SqlDataType.TIME != null)
        Assert.assertTrue(SqlDataType.TIME_WITH_TIME_ZONE != null)
    }
}
