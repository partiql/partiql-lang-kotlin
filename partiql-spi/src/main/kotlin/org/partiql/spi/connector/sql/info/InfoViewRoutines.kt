package org.partiql.spi.connector.sql.info

import org.partiql.spi.fn.FnIndex
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
internal class InfoViewRoutines(private val index: FnIndex) : InfoView {

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

    @PartiQLValueExperimental
    override fun value(): PartiQLValue {
        return bagValue(listOf(nullValue()))
    }
}
