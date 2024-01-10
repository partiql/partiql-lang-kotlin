package org.partiql.spi.internal.views

import org.partiql.spi.connector.ConnectorFunctionExperimental
import org.partiql.types.BagType
import org.partiql.types.StaticType
import org.partiql.types.StructType
import org.partiql.types.TupleConstraint
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.bagValue
import org.partiql.value.nullValue

/**
 * This provides the INFORMATION_SCHEMA.ROUTINES view for a [Connector].
 */
@OptIn(ConnectorFunctionExperimental::class)
internal class InfoViewRoutines : InfoView {

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
