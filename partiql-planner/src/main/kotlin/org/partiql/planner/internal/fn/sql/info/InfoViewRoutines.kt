package org.partiql.planner.internal.fn.sql.info

import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.Index
import org.partiql.types.BagType
import org.partiql.types.StaticType
import org.partiql.types.StructType
import org.partiql.types.TupleConstraint
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.bagValue
import org.partiql.value.nullValue

/**
 * This provides the INFORMATION_SCHEMA.ROUTINES view for an [SqlConnector].
 */
internal class InfoViewRoutines @OptIn(FnExperimental::class) constructor(private val index: Index<Fn>) : InfoView {

    override val schema: StaticType = BagType(
        elementType = StructType(
            fields = listOf(
                StructType.Field("ROUTINE_NAME", StaticType.STRING),
                StructType.Field("ROUTINE_TYPE", StaticType.STRING),
            ),
            contentClosed = true,
            constraints = setOf(TupleConstraint.Open(false))
        )
    )

    @OptIn(PartiQLValueExperimental::class)
    override fun value(): PartiQLValue {
        return bagValue(listOf(nullValue()))
    }
}
