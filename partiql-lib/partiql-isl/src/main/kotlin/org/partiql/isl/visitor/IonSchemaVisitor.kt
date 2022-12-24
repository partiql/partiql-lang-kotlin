package org.partiql.isl.visitor

import org.partiql.isl.Constraint
import org.partiql.isl.Definition
import org.partiql.isl.Footer
import org.partiql.isl.Header
import org.partiql.isl.Import
import org.partiql.isl.IonSchemaNode
import org.partiql.isl.Occurs
import org.partiql.isl.Range
import org.partiql.isl.Schema
import org.partiql.isl.Type
import org.partiql.isl.UserReservedFields
import org.partiql.isl.Value

public abstract class IonSchemaVisitor<R, C> {
    public open fun visit(node: Schema, ctx: C?): R? = defaultVisit(node, ctx)

    public open fun visit(node: Header, ctx: C?): R? = defaultVisit(node, ctx)

    public open fun visit(node: UserReservedFields, ctx: C?): R? = defaultVisit(node, ctx)

    public open fun visit(node: Footer, ctx: C?): R? = defaultVisit(node, ctx)

    public open fun visit(node: Import, ctx: C?): R? = when (node) {
        is Import.Schema -> visit(node, ctx)
        is Import.Type -> visit(node, ctx)
        is Import.TypeAlias -> visit(node, ctx)
    }

    public open fun visit(node: Import.Schema, ctx: C?): R? = defaultVisit(node, ctx)

    public open fun visit(node: Import.Type, ctx: C?): R? = defaultVisit(node, ctx)

    public open fun visit(node: Import.TypeAlias, ctx: C?): R? = defaultVisit(node, ctx)

    public open fun visit(node: Definition, ctx: C?): R? = defaultVisit(node, ctx)

    public open fun visit(node: Type, ctx: C?): R? = when (node) {
        is Type.Ref -> visit(node, ctx)
        is Type.Inline -> visit(node, ctx)
        is Type.Import -> visit(node, ctx)
    }

    public open fun visit(node: Type.Ref, ctx: C?): R? = defaultVisit(node, ctx)

    public open fun visit(node: Type.Inline, ctx: C?): R? = defaultVisit(node, ctx)

    public open fun visit(node: Type.Import, ctx: C?): R? = defaultVisit(node, ctx)

    public open fun visit(node: Constraint, ctx: C?): R? = when (node) {
        is Constraint.AllOf -> visit(node, ctx)
        is Constraint.AnyOf -> visit(node, ctx)
        is Constraint.Annotations -> visit(node, ctx)
        is Constraint.Length -> visit(node, ctx)
        is Constraint.Contains -> visit(node, ctx)
        is Constraint.Element -> visit(node, ctx)
        is Constraint.Exponent -> visit(node, ctx)
        is Constraint.FieldNames -> visit(node, ctx)
        is Constraint.Fields -> visit(node, ctx)
        is Constraint.Ieee754Float -> visit(node, ctx)
        is Constraint.Not -> visit(node, ctx)
        is Constraint.OneOf -> visit(node, ctx)
        is Constraint.OrderedElements -> visit(node, ctx)
        is Constraint.Precision -> visit(node, ctx)
        is Constraint.Regex -> visit(node, ctx)
        is Constraint.TimestampOffset -> visit(node, ctx)
        is Constraint.TimestampPrecision -> visit(node, ctx)
        is Constraint.Type -> visit(node, ctx)
        is Constraint.ValidValues -> visit(node, ctx)
    }

    public open fun visit(node: Constraint.AllOf, ctx: C?): R? = defaultVisit(node, ctx)

    public open fun visit(node: Constraint.AnyOf, ctx: C?): R? = defaultVisit(node, ctx)

    public open fun visit(node: Constraint.Annotations, ctx: C?): R? = when (node) {
        is Constraint.Annotations.Values -> visit(node, ctx)
        is Constraint.Annotations.Type -> visit(node, ctx)
    }

    public open fun visit(node: Constraint.Annotations.Values, ctx: C?): R? = defaultVisit(node, ctx)

    public open fun visit(node: Constraint.Annotations.Type, ctx: C?): R? = defaultVisit(node, ctx)

    public open fun visit(node: Constraint.Length, ctx: C?): R? = when (node) {
        is Constraint.Length.Equals -> visit(node, ctx)
        is Constraint.Length.Range -> visit(node, ctx)
    }

    public open fun visit(node: Constraint.Length.Equals, ctx: C?): R? = defaultVisit(node, ctx)

    public open fun visit(node: Constraint.Length.Range, ctx: C?): R? = defaultVisit(node, ctx)

    public open fun visit(node: Constraint.Contains, ctx: C?): R? = defaultVisit(node, ctx)

    public open fun visit(node: Constraint.Element, ctx: C?): R? = defaultVisit(node, ctx)

    public open fun visit(node: Constraint.Exponent, ctx: C?): R? = when (node) {
        is Constraint.Exponent.Equals -> visit(node, ctx)
        is Constraint.Exponent.Range -> visit(node, ctx)
    }

    public open fun visit(node: Constraint.Exponent.Equals, ctx: C?): R? = defaultVisit(node, ctx)

    public open fun visit(node: Constraint.Exponent.Range, ctx: C?): R? = defaultVisit(node, ctx)

    public open fun visit(node: Constraint.FieldNames, ctx: C?): R? = defaultVisit(node, ctx)

    public open fun visit(node: Constraint.Fields, ctx: C?): R? = defaultVisit(node, ctx)

    public open fun visit(node: Constraint.Ieee754Float, ctx: C?): R? = defaultVisit(node, ctx)

    public open fun visit(node: Constraint.Not, ctx: C?): R? = defaultVisit(node, ctx)

    public open fun visit(node: Constraint.OneOf, ctx: C?): R? = defaultVisit(node, ctx)

    public open fun visit(node: Constraint.OrderedElements, ctx: C?): R? = defaultVisit(node, ctx)

    public open fun visit(node: Constraint.Precision, ctx: C?): R? = when (node) {
        is Constraint.Precision.Equals -> visit(node, ctx)
        is Constraint.Precision.Range -> visit(node, ctx)
    }

    public open fun visit(node: Constraint.Precision.Equals, ctx: C?): R? = defaultVisit(node, ctx)

    public open fun visit(node: Constraint.Precision.Range, ctx: C?): R? = defaultVisit(node, ctx)

    public open fun visit(node: Constraint.Regex, ctx: C?): R? = defaultVisit(node, ctx)

    public open fun visit(node: Constraint.TimestampOffset, ctx: C?): R? = defaultVisit(node, ctx)

    public open fun visit(node: Constraint.TimestampPrecision, ctx: C?): R? = when (node) {
        is Constraint.TimestampPrecision.Equals -> visit(node, ctx)
        is Constraint.TimestampPrecision.Range -> visit(node, ctx)
    }

    public open fun visit(node: Constraint.TimestampPrecision.Equals, ctx: C?): R? =
        defaultVisit(node, ctx)

    public open fun visit(node: Constraint.TimestampPrecision.Range, ctx: C?): R? = defaultVisit(
        node,
        ctx
    )

    public open fun visit(node: Constraint.Type, ctx: C?): R? = defaultVisit(node, ctx)

    public open fun visit(node: Constraint.ValidValues, ctx: C?): R? = defaultVisit(node, ctx)

    public open fun visit(node: Value, ctx: C?): R? = when (node) {
        is Value.Ion -> visit(node, ctx)
        is Value.Range -> visit(node, ctx)
    }

    public open fun visit(node: Value.Ion, ctx: C?): R? = defaultVisit(node, ctx)

    public open fun visit(node: Value.Range, ctx: C?): R? = defaultVisit(node, ctx)

    public open fun visit(node: Range, ctx: C?): R? = when (node) {
        is Range.Int -> visit(node, ctx)
        is Range.Number -> visit(node, ctx)
        is Range.Timestamp -> visit(node, ctx)
        is Range.TimestampPrecision -> visit(node, ctx)
    }

    public open fun visit(node: Range.Int, ctx: C?): R? = defaultVisit(node, ctx)

    public open fun visit(node: Range.Number, ctx: C?): R? = defaultVisit(node, ctx)

    public open fun visit(node: Range.Timestamp, ctx: C?): R? = defaultVisit(node, ctx)

    public open fun visit(node: Range.TimestampPrecision, ctx: C?): R? = defaultVisit(node, ctx)

    public open fun visit(node: Occurs, ctx: C?): R? = when (node) {
        is Occurs.Equal -> visit(node, ctx)
        is Occurs.Range -> visit(node, ctx)
        is Occurs.Optional -> visit(node, ctx)
        is Occurs.Required -> visit(node, ctx)
    }

    public open fun visit(node: Occurs.Equal, ctx: C?): R? = defaultVisit(node, ctx)

    public open fun visit(node: Occurs.Range, ctx: C?): R? = defaultVisit(node, ctx)

    public open fun visit(node: Occurs.Optional, ctx: C?): R? = defaultVisit(node, ctx)

    public open fun visit(node: Occurs.Required, ctx: C?): R? = defaultVisit(node, ctx)

    public open fun defaultVisit(node: IonSchemaNode, ctx: C?): R? {
        for (child in node.children) {
            child.accept(this, ctx)
        }
        return null
    }
}
