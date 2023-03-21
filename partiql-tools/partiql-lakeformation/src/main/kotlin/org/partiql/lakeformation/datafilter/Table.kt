package org.partiql.lakeformation.datafilter

/**
 * For now, we model table as a list of string
 * For example: FROM a.b.c => Table(listof("a", "b", "c"))
 */
data class Table(val path: List<String>)
