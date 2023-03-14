/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at:
 *
 *       http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.planner.transforms

import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.ast.passes.SemanticException
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.errors.Problem
import org.partiql.lang.errors.ProblemHandler
import org.partiql.lang.errors.ProblemThrower
import org.partiql.lang.planner.PlanningProblemDetails
import org.partiql.lang.planner.transforms.impl.MetadataInference
import org.partiql.lang.planner.transforms.impl.PlannerContext
import org.partiql.lang.planner.transforms.plan.PlanTyper
import org.partiql.lang.planner.transforms.plan.PlanUtils
import org.partiql.lang.planner.transforms.plan.RelConverter
import org.partiql.lang.planner.transforms.plan.RexConverter
import org.partiql.lang.syntax.PartiQLParserBuilder
import org.partiql.plan.Rex
import org.partiql.spi.Plugin
import org.partiql.spi.sources.ColumnMetadata
import org.partiql.spi.sources.ValueDescriptor
import org.partiql.spi.sources.ValueDescriptor.TableDescriptor
import org.partiql.types.StaticType
import kotlin.jvm.Throws

/**
 * Infers the output schema of a query.
 */
public object PartiQLSchemaInferencer {

    /**
     * Infers a query's schema.
     */
    @JvmStatic
    @Throws(SemanticException::class)
    public fun infer(
        query: String,
        ctx: Context
    ): ValueDescriptor {
        return try {
            inferInternal(query, ctx)
        } catch (t: Throwable) {
            throw when (t) {
                is SemanticException -> t
                else -> SemanticException(
                    err = Problem(
                        SourceLocationMeta(0, 0),
                        PlanningProblemDetails.CompileError("Unhandled exception occurred.")
                    ),
                    cause = t
                )
            }
        }
    }

    /**
     * Context object required for performing schema inference.
     */
    public class Context(
        public val session: PlannerSession,
        plugins: List<Plugin>,
        public val problemHandler: ProblemHandler = ProblemThrower(),
    ) {
        internal val plannerContext = PlannerContext(MetadataInference(plugins, session.catalogConfig))
    }

    //
    //
    // INTERNAL
    //
    //

    private const val DEFAULT_TABLE_NAME = "UNSPECIFIED"

    private fun inferInternal(query: String, ctx: Context): ValueDescriptor {
        val parser = PartiQLParserBuilder.standard().build()
        val ast = parser.parseAstStatement(query)
        val normalizedAst = ast.normalize()
        return inferUsingLogicalPlan(normalizedAst, ctx)
    }

    /**
     * Infers using the logical plan.
     */
    private fun inferUsingLogicalPlan(normalizedAst: PartiqlAst.Statement, ctx: Context): ValueDescriptor {
        val query = normalizedAst as PartiqlAst.Statement.Query
        val plan = when (val expr = query.expr) {
            is PartiqlAst.Expr.Select -> RelConverter.convert(expr)
            else -> RexConverter.convert(expr)
        }
        val typedPlan = PlanTyper.type(
            plan,
            PlanTyper.Context(
                input = null,
                session = ctx.session,
                plannerCtx = ctx.plannerContext,
                scopingOrder = PlanTyper.ScopingOrder.LEXICAL_THEN_GLOBALS,
                allFunctions = emptyMap(),
                problemHandler = ctx.problemHandler
            )
        )
        return convertSchema(typedPlan)
    }

    private fun convertSchema(rex: Rex): ValueDescriptor = when (rex) {
        is Rex.Agg -> ValueDescriptor.TypeDescriptor(rex.type!!)
        is Rex.Binary -> ValueDescriptor.TypeDescriptor(rex.type!!)
        is Rex.Call -> ValueDescriptor.TypeDescriptor(rex.type!!)
        is Rex.Collection.Array -> ValueDescriptor.TypeDescriptor(rex.type!!)
        is Rex.Collection.Bag -> ValueDescriptor.TypeDescriptor(rex.type!!)
        is Rex.Id -> ValueDescriptor.TypeDescriptor(rex.type!!)
        is Rex.Lit -> ValueDescriptor.TypeDescriptor(rex.type!!)
        is Rex.Path -> ValueDescriptor.TypeDescriptor(rex.type!!)
        is Rex.Query.Collection -> when (rex.constructor) {
            null -> {
                val attrs = PlanUtils.getSchema(rex.rel).map { attr -> ColumnMetadata(attr.name, attr.type) }
                TableDescriptor(
                    name = DEFAULT_TABLE_NAME,
                    attributes = attrs
                )
            }
            else -> ValueDescriptor.TypeDescriptor(rex.type!!)
        }
        is Rex.Query.Scalar.Subquery -> ValueDescriptor.TypeDescriptor(rex.type!!)
        is Rex.Query.Scalar.Pivot -> ValueDescriptor.TypeDescriptor(rex.type!!)
        is Rex.Tuple -> ValueDescriptor.TypeDescriptor(rex.type!!)
        is Rex.Unary -> ValueDescriptor.TypeDescriptor(rex.type!!)
        is Rex.Switch -> ValueDescriptor.TypeDescriptor(StaticType.ANY) // TODO: Switch
    }
}
