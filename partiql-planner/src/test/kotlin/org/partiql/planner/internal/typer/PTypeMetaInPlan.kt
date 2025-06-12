package org.partiql.planner.internal.typer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.partiql.ast.Ast.exprCast
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.query
import org.partiql.ast.DataType
import org.partiql.ast.Literal
import org.partiql.ast.Statement
import org.partiql.plan.Action
import org.partiql.plan.rex.RexCast
import org.partiql.planner.PartiQLPlanner
import org.partiql.spi.catalog.Session
import org.partiql.spi.types.PType

private const val UNSPECIFIED_LENGTH = "UNSPECIFIED_LENGTH"
private const val UNSPECIFIED_PRECISION = "UNSPECIFIED_PRECISION"
private const val UNSPECIFIED_SCALE = "UNSPECIFIED_SCALE"

/**
 * Tests that PType metas are available in the plan after AST -> plan conversion and type inference.
 */
class PTypeMetaInPlan {
    private val planner = PartiQLPlanner.standard()
    private fun PType.assertHasLengthMeta() {
        assertEquals(this.metas[UNSPECIFIED_LENGTH], true)
    }

    private fun PType.assertHasPrecisionMeta() {
        assertEquals(this.metas[UNSPECIFIED_PRECISION], true)
    }

    private fun PType.assertHasScaleMeta() {
        assertEquals(this.metas[UNSPECIFIED_SCALE], true)
    }

    private fun createCastStatement(dt: DataType): Statement {
        return query(exprCast(exprLit(Literal.nul()), dt))
    }

    private fun getRexCast(dataType: DataType): RexCast {
        val castStatement = createCastStatement(dataType)
        val result = planner.plan(castStatement, Session.empty())
        val rex = (result.plan.action as Action.Query).rex
        rex as RexCast
        return rex
    }

    @Test
    fun `test decimal has no scale and no precision`() {
        val decimal = getRexCast(DataType.DECIMAL()).type.pType
        decimal.assertHasPrecisionMeta()
        decimal.assertHasScaleMeta()
    }

    @Test
    fun `test decimal has no scale`() {
        val decimal = getRexCast(DataType.DECIMAL(10)).type.pType
        decimal.assertHasScaleMeta()
    }

    @Test
    fun `test dec has no scale and no precision`() {
        val decimal = getRexCast(DataType.DEC()).type.pType
        decimal.assertHasPrecisionMeta()
        decimal.assertHasScaleMeta()
    }

    @Test
    fun `test dec has no scale`() {
        val decimal = getRexCast(DataType.DEC(10)).type.pType
        decimal.assertHasScaleMeta()
    }

    @Test
    fun `test numeric has no scale and no precision`() {
        val numeric = getRexCast(DataType.NUMERIC()).type.pType
        numeric.assertHasPrecisionMeta()
        numeric.assertHasScaleMeta()
    }

    @Test
    fun `test numeric has no scale`() {
        val numeric = getRexCast(DataType.NUMERIC(10)).type.pType
        numeric.assertHasScaleMeta()
    }

    @Test
    fun `test varchar has no length`() {
        val varchar = getRexCast(DataType.VARCHAR()).type.pType
        varchar.assertHasLengthMeta()
    }

    @Test
    fun `test character_varying has no length`() {
        val varchar = getRexCast(DataType.CHARACTER_VARYING()).type.pType
        varchar.assertHasLengthMeta()
    }

    @Test
    fun `test character has no length`() {
        val char = getRexCast(DataType.CHARACTER()).type.pType
        char.assertHasLengthMeta()
    }

    @Test
    fun `test char has no length`() {
        val char = getRexCast(DataType.CHAR()).type.pType
        char.assertHasLengthMeta()
    }

    @Test
    fun `test clob has no length`() {
        val clob = getRexCast(DataType.CLOB()).type.pType
        clob.assertHasLengthMeta()
    }

    @Test
    fun `test blob has no length`() {
        val blob = getRexCast(DataType.BLOB()).type.pType
        blob.assertHasLengthMeta()
    }

    @Test
    fun `test time has no precision`() {
        val time = getRexCast(DataType.TIME()).type.pType
        time.assertHasPrecisionMeta()
    }

    @Test
    fun `test timez has no precision`() {
        val timez = getRexCast(DataType.TIME_WITH_TIME_ZONE()).type.pType
        timez.assertHasPrecisionMeta()
    }

    @Test
    fun `test timestamp has no precision`() {
        val timestamp = getRexCast(DataType.TIMESTAMP()).type.pType
        timestamp.assertHasPrecisionMeta()
    }

    @Test
    fun `test timestampz has no precision`() {
        val timestampz = getRexCast(DataType.TIMESTAMP_WITH_TIME_ZONE()).type.pType
        timestampz.assertHasPrecisionMeta()
    }
}
