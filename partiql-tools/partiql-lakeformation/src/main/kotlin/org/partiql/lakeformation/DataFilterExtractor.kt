package org.partiql.lakeformation

import com.amazon.ionelement.api.AnyElement
import org.partiql.lakeformation.datafilter.ColumnFilter
import org.partiql.lakeformation.datafilter.LakeFormationDataFilter
import org.partiql.lakeformation.datafilter.Table
import org.partiql.lakeformation.exception.LakeFormationQueryUnsupportedException
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.prettyprint.QueryPrettyPrinter

/**
 * Taking in an AST, we validate Lake Formation Syntax requirement and extract:
 * The syntax requirement are:
 * 1. Query must be a SFW query, ddl, dml, exec, explain, and simple expression are not supported
 * 2. Projection items can only be identifier. (Qualified name (table.) and operations are not supported)
 * 3. From source must be in form of path; join, unpivot, subquery, etc. are not supported.
 *
 * See  for generated LakeFormationFilter.
 */
object DataFilterExtractor {

    fun extractDataFilter(statement: PartiqlAst.Statement): LakeFormationDataFilter {
        return when (statement) {
            is PartiqlAst.Statement.Query -> {
                when (statement.expr) {
                    is PartiqlAst.Expr.Select -> extractDataFilter(statement.expr as PartiqlAst.Expr.Select)
                    else -> throw LakeFormationQueryUnsupportedException("Only SFW query are supported in Lake Formation")
                }
            }
            is PartiqlAst.Statement.Dml -> throw LakeFormationQueryUnsupportedException("DML is not supported in Lake Formation")
            is PartiqlAst.Statement.Ddl -> throw LakeFormationQueryUnsupportedException("DDL is not supported in Lake Formation")
            is PartiqlAst.Statement.Exec -> throw LakeFormationQueryUnsupportedException("Exec is not supported in Lake Formation")
            is PartiqlAst.Statement.Explain -> throw LakeFormationQueryUnsupportedException("Explain is not supported in Lake Formation")
        }
    }

    private fun extractDataFilter(select: PartiqlAst.Expr.Select): LakeFormationDataFilter {
        return when {
            select.fromLet != null -> throw LakeFormationQueryUnsupportedException("FromLet is not supported by Lake Formation")
            select.group != null -> throw LakeFormationQueryUnsupportedException("Group By is not supported by Lake Formation")
            select.having != null -> throw LakeFormationQueryUnsupportedException("Having is not supported by Lake Formation")
            select.order != null -> throw LakeFormationQueryUnsupportedException("ORDER BY is not supported by Lake Formation")
            select.limit != null -> throw LakeFormationQueryUnsupportedException("LIMIT is not supported by Lake Formation")
            select.offset != null -> throw LakeFormationQueryUnsupportedException("OFFSET is not supported by Lake Formation")
            select.setq != null -> throw LakeFormationQueryUnsupportedException("Set Quantifier is not supported by Lake Formation")
            else -> {
                val sourceTable = extractSourceTable(select.from)
                val columnFilter = extractIncludeColumns(select.project)
                val rowFilter = select.where?.let(::reconstituteWhereClause)
                LakeFormationDataFilter(sourceTable, columnFilter, rowFilter)
            }
        }
    }

    private fun extractIncludeColumns(projection: PartiqlAst.Projection): ColumnFilter {
        return when (projection) {
            is PartiqlAst.Projection.ProjectList -> extractProjectItems(projection.projectItems)
            is PartiqlAst.Projection.ProjectStar -> ColumnFilter.allColumnsFilter()
            is PartiqlAst.Projection.ProjectPivot -> throw LakeFormationQueryUnsupportedException("Pivot is not supported by Lake Formation")
            is PartiqlAst.Projection.ProjectValue -> throw LakeFormationQueryUnsupportedException("SELECT VALUE is not supported by Lake Formation")
        }
    }

    private fun extractProjectItems(projectItems: Collection<PartiqlAst.ProjectItem>): ColumnFilter =
        ColumnFilter.includeColumnsFilter(projectItems.map(::getColumnName))

    // Assuming a generalized "lakeformationSyntaxValidation" will permit a from source similar to Andes's from source
    // but with arbitrary path steps.
    private fun extractSourceTable(fromSource: PartiqlAst.FromSource): Table {
        return when (fromSource) {
            is PartiqlAst.FromSource.Scan -> {
                when (val scanExpr = fromSource.expr) {
                    is PartiqlAst.Expr.Path -> {
                        val root =
                            extractIdExpr(scanExpr.root) ?: throw LakeFormationQueryUnsupportedException("Lake Formation From Source can only be a identifier or a path, but received $scanExpr")
                        val components = scanExpr.steps.map(::extractPathComponent)
                        val path = listOf(root) + components
                        Table(path)
                    }
                    is PartiqlAst.Expr.Id -> {
                        val tableName = extractIdExpr(scanExpr) ?: throw LakeFormationQueryUnsupportedException("Lake Formation From Source can only be a identifier or a path, but received $scanExpr")
                        Table(listOf(tableName))
                    }
                    else -> throw LakeFormationQueryUnsupportedException("Lake Formation From Source can only be a identifier or a path, but received $scanExpr")
                }
            }

            is PartiqlAst.FromSource.Join -> throw LakeFormationQueryUnsupportedException("Join is not supported by Lake Formation")
            is PartiqlAst.FromSource.Unpivot -> throw LakeFormationQueryUnsupportedException("Unpivot is not supported by Lake Formation")
        }
    }

    private fun extractPathComponent(step: PartiqlAst.PathStep): String {
        return when (step) {
            is PartiqlAst.PathStep.PathExpr ->
                extractLit(step.index)?.stringValueOrNull
                    ?: throw LakeFormationQueryUnsupportedException("Invalid path step, expect an literal but received ${step.index}")

            is PartiqlAst.PathStep.PathUnpivot -> throw LakeFormationQueryUnsupportedException("Path Unpivot is not supported in Lake Formation")
            is PartiqlAst.PathStep.PathWildcard -> throw LakeFormationQueryUnsupportedException("Path Wildcard is not supported in Lake Formation")
        }
    }

    private fun getColumnName(item: PartiqlAst.ProjectItem): String {
        return when (item) {
            is PartiqlAst.ProjectItem.ProjectExpr -> toColumnName(item)
            is PartiqlAst.ProjectItem.ProjectAll -> throw LakeFormationQueryUnsupportedException("Unsupported Projection Item in Lake Formation")
        }
    }

    private fun toColumnName(projectExpr: PartiqlAst.ProjectItem.ProjectExpr): String {
        return extractIdExpr(projectExpr.expr) ?: throw LakeFormationQueryUnsupportedException("Column name must be an identifier in LakeFormation")
    }

    private fun reconstituteWhereClause(whereExpr: PartiqlAst.Expr): String {
        val query = PartiqlAst.build {
            PartiqlAst.Statement.Query(
                whereExpr
            )
        }
        return QueryPrettyPrinter().astToPrettyQuery(query)
    }

    private fun extractIdExpr(expr: PartiqlAst.Expr): String? {
        return when (expr) {
            is PartiqlAst.Expr.Id -> expr.name.text
            else -> null
        }
    }

    private fun extractLit(expr: PartiqlAst.Expr): AnyElement? {
        return when (expr) {
            is PartiqlAst.Expr.Lit -> expr.value
            else -> null
        }
    }
}
