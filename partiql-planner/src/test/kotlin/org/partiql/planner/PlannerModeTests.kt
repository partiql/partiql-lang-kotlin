package org.partiql.planner

import org.junit.jupiter.api.Test
import org.partiql.ast.Statement
import org.partiql.errors.Problem
import org.partiql.errors.ProblemSeverity
import org.partiql.parser.PartiQLParserBuilder
import org.partiql.plan.debug.PlanPrinter
import org.partiql.planner.util.ProblemCollector
import org.partiql.plugins.memory.MemoryCatalog
import org.partiql.plugins.memory.MemoryConnector
import org.partiql.spi.connector.ConnectorSession
import org.partiql.types.BagType
import org.partiql.types.StaticType
import org.partiql.types.StructType

internal class PlannerModeTests {
    final val catalogName = "mode_test"
    final val userId = "test-user"
    final val queryId = "query"

    val catalog = MemoryCatalog
        .PartiQL()
        .name(catalogName)
        .define("missing_binding", StaticType.MISSING)
        .define("atomic", StaticType.INT2)
        .define("collection_no_missing_atomic", BagType(StaticType.INT2))
        .define("collection_contain_missing_atomic", BagType(StaticType.unionOf(StaticType.INT2, StaticType.MISSING)))
        .define("struct_no_missing", StructType(listOf(StructType.Field("f1", StaticType.INT2))))
        .define("struct_with_missing", StructType(listOf(StructType.Field("f1", StaticType.unionOf(StaticType.INT2, StaticType.MISSING)))))
        .build()

    val metadata = MemoryConnector(catalog).getMetadata(
        object : ConnectorSession {
            override fun getQueryId(): String = "q"
            override fun getUserId(): String = "s"
        }
    )

    val session: ((PartiQLPlanner.Session.MissingOpBehavior) -> PartiQLPlanner.Session) = { mode ->
        PartiQLPlanner.Session(
            queryId = queryId,
            userId = userId,
            currentCatalog = catalogName,
            catalogs = mapOf(catalogName to metadata),
            missingOpBehavior = mode
        )
    }

    val parser = PartiQLParserBuilder().build()
    val planner = PartiQLPlanner.debug()

    val statement: ((String) -> Statement) = { query ->
        parser.parse(query).root
    }

    fun assertProblem(
        plan: org.partiql.plan.PlanNode,
        problems: List<Problem>,
        block: () -> Boolean
    ) {
        assert(block.invoke()) {
            buildString {
                this.appendLine("Expected 1 Error but received: ")
                val errors = problems.filter { it.details.severity == ProblemSeverity.ERROR }
                if (errors.isEmpty()) this.appendLine("No Problem")
                errors.forEach {
                    this.appendLine(it.toString())
                }

                this.appendLine("--------Plan---------")
                PlanPrinter.append(this, plan)
            }
        }.also {
            println(
                buildString {
                    this.appendLine("--------Plan---------")
                    PlanPrinter.append(this, plan)
                }
            )

            println(
                buildString {
                    this.appendLine("----------problems---------")
                    problems.forEach {
                        this.appendLine(it.toString())
                    }
                }
            )
        }
    }

    @Test
    fun missingLiteralQuite() {
        val session = session(PartiQLPlanner.Session.MissingOpBehavior.QUIET)
        val query = "MISSING"
        val pc = ProblemCollector()

        val res = planner.plan(statement(query), session, pc)
        val problems = pc.problems
        val plan = res.plan

        assertProblem(plan, problems) {
            problems.none { it.details.severity == ProblemSeverity.ERROR }
        }
        assert((plan.statement as org.partiql.plan.Statement.Query).root.type == StaticType.MISSING)
    }

    @Test
    fun missingLiteralSignal() {
        val session = session(PartiQLPlanner.Session.MissingOpBehavior.SIGNAL)
        val query = "MISSING"
        val pc = ProblemCollector()

        val res = planner.plan(statement(query), session, pc)
        val problems = pc.problems
        val plan = res.plan

        assertProblem(plan, problems) {
            problems.none { it.details.severity == ProblemSeverity.ERROR }
        }
        assert((plan.statement as org.partiql.plan.Statement.Query).root.type == StaticType.MISSING)
    }

    @Test
    fun missingLiteralQuiteFunction() {
        val session = session(PartiQLPlanner.Session.MissingOpBehavior.QUIET)
        val query = "1 + MISSING"
        val pc = ProblemCollector()

        val res = planner.plan(statement(query), session, pc)
        val problems = pc.problems
        val plan = res.plan

        assertProblem(plan, problems) {
            problems.none { it.details.severity == ProblemSeverity.ERROR }
        }
        assert((plan.statement as org.partiql.plan.Statement.Query).root.type == StaticType.MISSING)
    }

    @Test
    fun missingLiteralSignalFunction() {
        val session = session(PartiQLPlanner.Session.MissingOpBehavior.SIGNAL)
        val query = "1 + MISSING"
        val pc = ProblemCollector()

        val res = planner.plan(statement(query), session, pc)
        val problems = pc.problems
        val plan = res.plan

        assertProblem(plan, problems) {
            problems.filter { it.details.severity == ProblemSeverity.ERROR }.size == 1
        }
        assert((plan.statement as org.partiql.plan.Statement.Query).root.type == StaticType.MISSING)
    }

    @Test
    fun missingLiteralQuiteNaviSymbol() {
        val session = session(PartiQLPlanner.Session.MissingOpBehavior.QUIET)
        val query = "MISSING.a"
        val pc = ProblemCollector()

        val res = planner.plan(statement(query), session, pc)
        val problems = pc.problems
        val plan = res.plan

        assertProblem(plan, problems) {
            problems.none { it.details.severity == ProblemSeverity.ERROR }
        }
        assert((plan.statement as org.partiql.plan.Statement.Query).root.type == StaticType.MISSING)
    }

    @Test
    fun missingLiteralSignalNaviSymbol() {
        val session = session(PartiQLPlanner.Session.MissingOpBehavior.SIGNAL)
        val query = "MISSING.a"
        val pc = ProblemCollector()

        val res = planner.plan(statement(query), session, pc)
        val problems = pc.problems
        val plan = res.plan

        assertProblem(plan, problems) {
            problems.filter { it.details.severity == ProblemSeverity.ERROR }.size == 1
        }
        assert((plan.statement as org.partiql.plan.Statement.Query).root.type == StaticType.MISSING)
    }

    @Test
    fun missingLiteralQuiteNaviIndex() {
        val session = session(PartiQLPlanner.Session.MissingOpBehavior.QUIET)
        val query = "MISSING[0]"
        val pc = ProblemCollector()

        val res = planner.plan(statement(query), session, pc)
        val problems = pc.problems
        val plan = res.plan

        assertProblem(plan, problems) {
            problems.none { it.details.severity == ProblemSeverity.ERROR }
        }
        assert((plan.statement as org.partiql.plan.Statement.Query).root.type == StaticType.MISSING)
    }

    @Test
    fun missingLiteralSignalNaviIndex() {
        val session = session(PartiQLPlanner.Session.MissingOpBehavior.SIGNAL)
        val query = "MISSING[0]"
        val pc = ProblemCollector()

        val res = planner.plan(statement(query), session, pc)
        val problems = pc.problems
        val plan = res.plan

        assertProblem(plan, problems) {
            problems.filter { it.details.severity == ProblemSeverity.ERROR }.size == 1
        }
        assert((plan.statement as org.partiql.plan.Statement.Query).root.type == StaticType.MISSING)
    }

    @Test
    fun missingLiteralQuiteNaviKey() {
        val session = session(PartiQLPlanner.Session.MissingOpBehavior.QUIET)
        val query = "MISSING['a']"
        val pc = ProblemCollector()

        val res = planner.plan(statement(query), session, pc)
        val problems = pc.problems
        val plan = res.plan

        assertProblem(plan, problems) {
            problems.none { it.details.severity == ProblemSeverity.ERROR }
        }
        assert((plan.statement as org.partiql.plan.Statement.Query).root.type == StaticType.MISSING)
    }

    @Test
    fun missingLiteralSignalNaviKey() {
        val session = session(PartiQLPlanner.Session.MissingOpBehavior.SIGNAL)
        val query = "MISSING['a']"
        val pc = ProblemCollector()

        val res = planner.plan(statement(query), session, pc)
        val problems = pc.problems
        val plan = res.plan

        assertProblem(plan, problems) {
            problems.filter { it.details.severity == ProblemSeverity.ERROR }.size == 1
        }
        assert((plan.statement as org.partiql.plan.Statement.Query).root.type == StaticType.MISSING)
    }

    @Test
    fun chainedPathQuite() {
        val session = session(PartiQLPlanner.Session.MissingOpBehavior.QUIET)
        val query = "MISSING['a'].a"
        val pc = ProblemCollector()

        val res = planner.plan(statement(query), session, pc)
        val problems = pc.problems
        val plan = res.plan

        assertProblem(plan, problems) {
            problems.none { it.details.severity == ProblemSeverity.ERROR }
        }
        assert((plan.statement as org.partiql.plan.Statement.Query).root.type == StaticType.MISSING)
    }

    @Test
    fun chainedPathSignal() {
        val session = session(PartiQLPlanner.Session.MissingOpBehavior.SIGNAL)
        val query = "MISSING['a'].a"
        val pc = ProblemCollector()

        val res = planner.plan(statement(query), session, pc)
        val problems = pc.problems
        val plan = res.plan

        assertProblem(plan, problems) {
            problems.filter { it.details.severity == ProblemSeverity.ERROR }.size == 2
        }
        assert((plan.statement as org.partiql.plan.Statement.Query).root.type == StaticType.MISSING)
    }

    @Test
    fun unresolvedFuncQuite() {
        val session = session(PartiQLPlanner.Session.MissingOpBehavior.QUIET)
        val query = "not_a_func(1)"
        val pc = ProblemCollector()

        val res = planner.plan(statement(query), session, pc)
        val problems = pc.problems
        val plan = res.plan

        assertProblem(plan, problems) {
            problems.filter { it.details.severity == ProblemSeverity.ERROR }.size == 1
        }
        assert((plan.statement as org.partiql.plan.Statement.Query).root.type == StaticType.ANY)
    }

    @Test
    fun unresolvedFuncSignal() {
        val session = session(PartiQLPlanner.Session.MissingOpBehavior.SIGNAL)
        val query = "not_a_func(1)"
        val pc = ProblemCollector()

        val res = planner.plan(statement(query), session, pc)
        val problems = pc.problems
        val plan = res.plan

        assertProblem(plan, problems) {
            problems.filter { it.details.severity == ProblemSeverity.ERROR }.size == 1
        }
        assert((plan.statement as org.partiql.plan.Statement.Query).root.type == StaticType.ANY)
    }

    @Test
    fun continuation() {
        val session = session(PartiQLPlanner.Session.MissingOpBehavior.SIGNAL)
        val query = "1 + not_a_func(1)"
        val pc = ProblemCollector()

        val res = planner.plan(statement(query), session, pc)
        val problems = pc.problems
        val plan = res.plan

        assertProblem(plan, problems) {
            problems.filter { it.details.severity == ProblemSeverity.ERROR }.size == 1
        }

        // TODO: PLUS OP does not return INT2
        val expectedType = StaticType.unionOf(
            StaticType.INT4,
            StaticType.INT8,
            StaticType.INT8,
            StaticType.INT,
            StaticType.FLOAT,
            StaticType.DECIMAL, // Parameter?
            StaticType.MISSING,
//            StaticType.NULL // TODO: There is a bug in function resolution, null type is not there.
        ).flatten()
        expectedType.assertStaticTypeEqual((plan.statement as org.partiql.plan.Statement.Query).root.type)
    }

    private fun StaticType.assertStaticTypeEqual(other: StaticType) {
        val thisAll = this.allTypes.toSet()
        val otherAll = other.allTypes.toSet()
        val diff = (thisAll - otherAll) + (otherAll - thisAll)
        assert(diff.isEmpty()) {
            buildString {
                this.appendLine("expected: ")
                thisAll.forEach {
                    this.append("$it, ")
                }
                this.appendLine()
                this.appendLine("actual")
                otherAll.forEach {
                    this.append("$it, ")
                }
                this.appendLine()
                this.appendLine("diff")
                diff.forEach {
                    this.append("$it, ")
                }
            }
        }
    }
}
