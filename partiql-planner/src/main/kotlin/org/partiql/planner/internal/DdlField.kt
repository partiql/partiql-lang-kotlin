package org.partiql.planner.internal

import org.partiql.planner.internal.ir.Statement
import org.partiql.planner.internal.ir.statementDDLAttribute
import org.partiql.planner.internal.typer.CompilerType
import org.partiql.spi.catalog.Identifier
import org.partiql.types.Field
import org.partiql.types.PType
import org.partiql.types.shape.PShape

/**
 * An implementation for [Field] that is used by DDL
 * to hold additional information in the struct field.
 * It is identical to [Statement.DDL.Attribute]
 */
internal data class DdlField(
    val name: Identifier,
    val type: PShape,
    val isNullable: Boolean,
    val isOptional: Boolean,
    val constraints: List<Statement.DDL.Constraint>,
    val isPrimaryKey: Boolean,
    val isUnique: Boolean,
    val comment: String?
) : Field {

    override fun getName(): String {
        return name.getIdentifier().getText()
    }

    override fun getType(): PType {
        return type
    }

    fun toAttr() =
        statementDDLAttribute(
            this.name,
            this.type,
            this.isNullable,
            this.isOptional,
            this.isPrimaryKey,
            this.isUnique,
            this.constraints,
            this.comment
        )

    companion object {
        fun fromAttr(attr: Statement.DDL.Attribute): DdlField =
            DdlField(attr.name, attr.type, attr.isNullable, attr.isOptional, attr.constraints, attr.isPrimaryKey, attr.isUnique, attr.comment)
    }
}
