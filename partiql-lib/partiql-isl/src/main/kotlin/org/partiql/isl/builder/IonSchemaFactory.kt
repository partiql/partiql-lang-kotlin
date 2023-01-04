package org.partiql.isl.builder

import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.TimestampElement
import org.partiql.isl.Bounds
import org.partiql.isl.Constraint
import org.partiql.isl.Definition
import org.partiql.isl.Footer
import org.partiql.isl.Header
import org.partiql.isl.Import
import org.partiql.isl.Measure
import org.partiql.isl.Occurs
import org.partiql.isl.Range
import org.partiql.isl.RegexFlag
import org.partiql.isl.Schema
import org.partiql.isl.TimestampPrecision
import org.partiql.isl.Type
import org.partiql.isl.UserReservedFields
import org.partiql.isl.Value
import org.partiql.isl.Version

public abstract class IonSchemaFactory {
    public open fun schema(
        version: Version,
        header: Header?,
        definitions: List<Definition>,
        footer: Footer?
    ) = Schema(version, header, definitions, footer)

    public open fun header(imports: List<Import>, userReservedFields: UserReservedFields) =
        Header(imports, userReservedFields)

    public open fun userReservedFields(
        header: List<String>,
        type: List<String>,
        footer: List<String>
    ) = UserReservedFields(header, type, footer)

    public open fun footer() = Footer()

    public open fun importSchema(id: String) = Import.Schema(id)

    public open fun importType(id: String, type: String) = Import.Type(id, type)

    public open fun importTypeAlias(
        id: String,
        type: String,
        alias: String
    ) = Import.TypeAlias(id, type, alias)

    public open fun definition(name: String, constraints: List<Constraint>) = Definition(
        name,
        constraints
    )

    public open fun typeRef(name: String, nullable: Boolean) = Type.Ref(name, nullable)

    public open fun typeInline(
        constraints: List<Constraint>,
        nullable: Boolean,
        occurs: Occurs?
    ) = Type.Inline(constraints, nullable, occurs)

    public open fun typeImport(
        schema: String,
        type: String,
        nullable: Boolean
    ) = Type.Import(schema, type, nullable)

    public open fun constraintAllOf(types: List<Type>) = Constraint.AllOf(types)

    public open fun constraintAnyOf(types: List<Type>) = Constraint.AnyOf(types)

    public open fun constraintAnnotationsValues(
        modifier: Constraint.Annotations.Values.Modifier,
        values: List<String>
    ) = Constraint.Annotations.Values(modifier, values)

    public open fun constraintAnnotationsType(type: Type) = Constraint.Annotations.Type(type)

    public open fun constraintLengthEquals(measure: Measure, length: Long) =
        Constraint.Length.Equals(measure, length)

    public open fun constraintLengthRange(measure: Measure, range: Range.Int) =
        Constraint.Length.Range(measure, range)

    public open fun constraintContains(values: List<Value.Ion>) = Constraint.Contains(values)

    public open fun constraintElement(type: Type, distinct: Boolean?) = Constraint.Element(
        type,
        distinct
    )

    public open fun constraintExponentEquals(`value`: Long) = Constraint.Exponent.Equals(value)

    public open fun constraintExponentRange(range: Range.Int) = Constraint.Exponent.Range(range)

    public open fun constraintFieldNames(type: Constraint.Type, distinct: Boolean?) =
        Constraint.FieldNames(type, distinct)

    public open fun constraintFields(closed: Boolean?, fields: Map<String, Type>) =
        Constraint.Fields(closed, fields)

    public open fun constraintIeee754Float(format: Constraint.Ieee754Float.Format) =
        Constraint.Ieee754Float(format)

    public open fun constraintNot(type: Constraint.Type) = Constraint.Not(type)

    public open fun constraintOneOf(types: List<Type>) = Constraint.OneOf(types)

    public open fun constraintOrderedElements(types: List<Type>) = Constraint.OrderedElements(types)

    public open fun constraintPrecisionEquals(`value`: Long) = Constraint.Precision.Equals(value)

    public open fun constraintPrecisionRange(range: Range.Int) = Constraint.Precision.Range(range)

    public open fun constraintRegex(pattern: String, flags: List<RegexFlag>) =
        Constraint.Regex(pattern, flags)

    public open fun constraintTimestampOffset(offsets: List<String>) =
        Constraint.TimestampOffset(offsets)

    public open fun constraintTimestampPrecisionEquals(`value`: TimestampPrecision) =
        Constraint.TimestampPrecision.Equals(value)

    public open fun constraintTimestampPrecisionRange(range: Range.TimestampPrecision) =
        Constraint.TimestampPrecision.Range(range)

    public open fun constraintType(type: Type) = Constraint.Type(type)

    public open fun constraintValidValues(values: List<Value>) = Constraint.ValidValues(values)

    public open fun valueIon(`value`: IonElement) = Value.Ion(value)

    public open fun valueRange(`value`: Range) = Value.Range(value)

    public open fun rangeInt(
        lower: Long?,
        upper: Long?,
        bounds: Bounds
    ) = Range.Int(lower, upper, bounds)

    public open fun rangeNumber(
        lower: Double?,
        upper: Double?,
        bounds: Bounds
    ) = Range.Number(lower, upper, bounds)

    public open fun rangeTimestamp(
        lower: TimestampElement?,
        upper: TimestampElement?,
        bounds: Bounds
    ) = Range.Timestamp(lower, upper, bounds)

    public open fun rangeTimestampPrecision(
        lower: TimestampPrecision?,
        upper: TimestampPrecision?,
        bounds: Bounds
    ) = Range.TimestampPrecision(lower, upper, bounds)

    public open fun occursEqual(`value`: Long) = Occurs.Equal(value)

    public open fun occursRange(range: Range.Int) = Occurs.Range(range)

    public open fun occursOptional() = Occurs.Optional()

    public open fun occursRequired() = Occurs.Required()

    public companion object {
        public val DEFAULT: IonSchemaFactory = object : IonSchemaFactory() {}
    }
}
