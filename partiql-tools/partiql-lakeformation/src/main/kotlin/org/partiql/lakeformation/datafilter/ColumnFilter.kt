package org.partiql.lakeformation.datafilter

/**
 * Element of a produced LakeFormation Data Filter.
 *
 * In reality, Column-level access can be set by one of the following:
 * 1. Access to all columns:
 *    1. Filter won't have any column restrictions.
 *    2. corresponding to SQL's SELECT *
 * 2. Include column:
 *    1. Filter will only access to specific columns
 *    2. corresponding to SQL's SELECT var1, var2
 * 3. Exclude column:
 *    1.Filter will allow access to all but specific columns.
 *    2. There is no directly SQL corresponding statement at least today.
 *    3. PartiQL does not support anything like that at least today.
 *    4. we leave the [excludeColumnNames] in the class, but it should stay as an empty list.
 */
data class ColumnFilter(
    val includeColumnNames: List<String>,
    val excludeColumnNames: List<String> = emptyList(),
    val includeAllColumns: Boolean?
) {
    companion object {
        fun includeColumnsFilter(columnNames: List<String>): ColumnFilter {
            return ColumnFilter(
                includeColumnNames = columnNames,
                excludeColumnNames = emptyList(),
                includeAllColumns = false
            )
        }

        fun includeColumnsFilter(vararg columnNames: String): ColumnFilter {
            return includeColumnsFilter(columnNames.toList())
        }

        fun allColumnsFilter(): ColumnFilter {
            return ColumnFilter(
                includeColumnNames = emptyList(),
                excludeColumnNames = emptyList(),
                includeAllColumns = true
            )
        }
    }
}
