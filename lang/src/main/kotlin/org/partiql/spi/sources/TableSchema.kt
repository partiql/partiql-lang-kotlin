package org.partiql.spi.sources

public class TableSchema(
    public val root: String,
    public val steps: List<String>,
    public val ordering: SchemaOrdering,
    public val attributeOrdering: AttributeOrdering,
    public val attributes: List<ColumnMetadata>
) {

    public enum class SchemaOrdering {
        ORDERED,
        UNORDERED
    }

    public enum class AttributeOrdering {
        ORDERED,
        UNORDERED
    }
}
