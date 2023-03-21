package org.partiql.lakeformation.datafilter

/**
 * Lake Formation Data Filter
 *
 * @param sourceTable : The from source within the SFW query. See [Table]
 * @param columnFilter: The projected items in the SFW query. See [ColumnFilter]
 * @param rowFilter: A string representation of the where clause in the SFW query.
 */
data class LakeFormationDataFilter(
    val sourceTable: Table,
    val columnFilter: ColumnFilter,
    val rowFilter: String?
)
