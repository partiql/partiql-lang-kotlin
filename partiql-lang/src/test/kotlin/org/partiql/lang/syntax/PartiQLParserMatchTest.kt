package org.partiql.lang.syntax

import com.amazon.ionelement.api.ionBool
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionString
import org.junit.Test
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.defnidReg
import org.partiql.lang.domains.vr
import kotlin.test.assertFailsWith

class PartiQLParserMatchTest : PartiQLParserTestBase() {

    override val targets: Array<ParserTarget> = arrayOf(ParserTarget.DEFAULT, ParserTarget.EXPERIMENTAL)

    @Test
    fun loneMatchExpr1path() = assertExpression(
        "(MyGraph MATCH (x))"
    ) {
        astMygraphMatchAllNodes
    }

    @Test
    fun loneMatchExpr1path_noParens() {
        // fails because it should be in parentheses
        assertFailsWith<ParserException> {
            assertExpression(
                "MyGraph MATCH (x)"
            ) {
                astMygraphMatchAllNodes
            }
        }
    }

    @Test
    fun loneMatchExpr2path() = assertExpression(
        "( MyGraph MATCH (x), -[u]-> )"
    ) {
        astMyGraphMatchAllNodesEdges
    }

    @Test
    fun loneMatchExpr2path_noParens() {
        // fails because it should be in parentheses
        assertFailsWith<ParserException> {
            assertExpression(
                "MyGraph MATCH (x), -[u]-> "
            ) {
                astMyGraphMatchAllNodesEdges
            }
        }
    }

    // `MyGraph MATCH (x), -[u]->`
    private val astMyGraphMatchAllNodesEdges = PartiqlAst.build {
        graphMatch(
            expr = vr("MyGraph"),
            gpmlPattern = gpmlPattern(
                patterns = listOf(
                    graphMatchPattern(
                        parts = listOf(
                            node(
                                prefilter = null,
                                variable = defnidReg("x"),
                            )
                        )
                    ),
                    graphMatchPattern(
                        parts = listOf(
                            edge(
                                direction = edgeRight(),
                                variable = defnidReg("u")
                            )
                        )
                    )
                )
            )
        )
    }

    // `MyGraph MATCH (x)`
    private val astMygraphMatchAllNodes = PartiqlAst.build {
        graphMatch(
            expr = vr("MyGraph"),
            gpmlPattern = gpmlPattern(
                patterns = listOf(
                    graphMatchPattern(
                        parts = listOf(
                            node(
                                prefilter = null,
                                variable = defnidReg("x"),
                            )
                        )
                    )
                )
            )
        )
    }

    // `SELECT * FROM tbl1`
    private val astSelectStarFromTbl1 = PartiqlAst.build {
        select(
            project = projectStar(),
            from = scan(vr("tbl1"))
        )
    }

    @Test
    fun leftMatchExprInUnion() = assertExpression(
        "(MyGraph MATCH (x)) UNION SELECT * FROM tbl1"
    ) {
        bagOp(
            op = union(),
            quantifier = distinct(),
            operands = listOf(
                astMygraphMatchAllNodes,
                astSelectStarFromTbl1
            )
        )
    }

    @Test
    fun leftMatchExprInUnion_noParens() {
        // fails because it should be in parentheses
        assertFailsWith<ParserException> {
            assertExpression(
                "MyGraph MATCH (x) UNION SELECT * FROM tbl1"
            ) {
                bagOp(
                    op = union(),
                    quantifier = distinct(),
                    operands = listOf(
                        astMygraphMatchAllNodes,
                        astSelectStarFromTbl1
                    )
                )
            }
        }
    }

    @Test
    fun rightMatchExprInUnion() = assertExpression(
        "SELECT * FROM tbl1 UNION (MyGraph MATCH (x))"
    ) {
        bagOp(
            op = union(),
            quantifier = distinct(),
            operands = listOf(
                astSelectStarFromTbl1,
                astMygraphMatchAllNodes
            )
        )
    }

    @Test
    fun rightMatchExprInUnion_noParens() {
        // fails because it should be in parentheses
        assertFailsWith<ParserException> {
            assertExpression(
                "SELECT * FROM tbl1 UNION MyGraph MATCH (x)"
            ) {
                bagOp(
                    op = union(),
                    quantifier = distinct(),
                    operands = listOf(
                        astSelectStarFromTbl1,
                        astMygraphMatchAllNodes
                    )
                )
            }
        }
    }

    @Test
    fun matchLeftTight() = assertExpression(
        "3 + (MyGraph MATCH (x))"
    ) {
        plus(
            lit(ionInt(3)),
            astMygraphMatchAllNodes
        )
    }

    @Test
    fun matchLeftTight_noParens() {
        // fails because it should be in parentheses
        assertFailsWith<ParserException> {
            assertExpression(
                "3 + MyGraph MATCH (x)"
            ) {
                plus(
                    lit(ionInt(3)),
                    astMygraphMatchAllNodes
                )
            }
        }
    }

    @Test
    fun allNodesNoLabel() = assertExpression(
        "SELECT 1 FROM my_graph MATCH ()"
    ) {
        select(
            project = projectList(projectExpr(lit(ionInt(1)))),
            from = scan(
                graphMatch(
                    expr = vr("my_graph"),
                    gpmlPattern = gpmlPattern(
                        patterns = listOf(
                            graphMatchPattern(
                                parts = listOf(
                                    node(
                                        prefilter = null,
                                        variable = null,
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            where = null
        )
    }

    @Test
    fun starAllNodesNoLabel() = assertExpression(
        "SELECT * FROM my_graph MATCH ()"
    ) {
        select(
            project = projectStar(),
            from = scan(
                graphMatch(
                    expr = vr("my_graph"),
                    gpmlPattern = gpmlPattern(
                        patterns = listOf(
                            graphMatchPattern(
                                parts = listOf(
                                    node(
                                        prefilter = null,
                                        variable = null,
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
    }

    @Test
    fun allNodesNoLabelFilter() = assertExpression(
        "SELECT 1 FROM my_graph MATCH () WHERE contains_value('1')",
    ) {
        select(
            project = projectList(projectExpr(lit(ionInt(1)))),
            from = scan(
                graphMatch(
                    expr = vr("my_graph"),
                    gpmlPattern = gpmlPattern(
                        patterns = listOf(
                            graphMatchPattern(
                                parts = listOf(
                                    node(
                                        prefilter = null,
                                        variable = null,
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            where = call(funcName = defnidReg("contains_value"), args = listOf(lit(ionString("1"))))
        )
    }

    val bindAllNodesAST =
        { projection: PartiqlAst.Projection, asVar: PartiqlAst.Defnid? ->
            PartiqlAst.build {
                select(
                    project = projection,
                    from = scan(
                        expr = graphMatch(
                            expr = vr("my_graph"),
                            gpmlPattern = gpmlPattern(
                                patterns = listOf(
                                    graphMatchPattern(
                                        parts = listOf(
                                            node(
                                                prefilter = null,
                                                variable = defnidReg("x"),
                                            )
                                        )
                                    )
                                )
                            )
                        ),
                        asAlias = asVar
                    )
                )
            }
        }

    @Test
    fun bindAllNodesProjectBound() = assertExpression(
        "SELECT x FROM my_graph MATCH (x)"
    ) {
        bindAllNodesAST(
            projectList(projectExpr(expr = vr("x"))),
            null
        )
    }

    @Test
    fun bindAllNodesProjectStar() = assertExpression(
        "SELECT * FROM my_graph MATCH (x)"
    ) {
        bindAllNodesAST(
            projectStar(),
            null
        )
    }

    @Test
    fun bindAllNodesProjectBoundWithAS() = assertExpression(
        "SELECT * FROM my_graph MATCH (x) AS a"
    ) {
        bindAllNodesAST(
            projectStar(),
            defnidReg("a")
        )
    }

    @Test
    fun bindAllNodesProjectBoundWithParensAS() = assertExpression(
        "SELECT * FROM (my_graph MATCH (x)) AS a"
    ) {
        bindAllNodesAST(
            projectStar(),
            defnidReg("a")
        )
    }

    @Test
    fun allNodes() = assertExpression(
        "SELECT x.info AS info FROM my_graph MATCH (x) WHERE x.name LIKE 'foo'",
    ) {
        select(
            project = projectList(
                projectExpr(
                    expr = path(vr("x"), pathExpr(lit(ionString("info")), regular())),
                    asAlias = defnidReg("info")
                )
            ),
            from = scan(
                graphMatch(
                    expr = vr("my_graph"),
                    gpmlPattern = gpmlPattern(
                        patterns = listOf(
                            graphMatchPattern(
                                parts = listOf(
                                    node(
                                        prefilter = null,
                                        variable = defnidReg("x"),
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            where = like(
                value = path(vr("x"), pathExpr(lit(ionString("name")), regular())),
                pattern = lit(ionString("foo"))
            )
        )
    }

    @Test
    fun labelledNodes() = assertExpression(
        "SELECT x AS target FROM my_graph MATCH (x:Label) WHERE x.has_data = true",
    ) {
        select(
            project = projectList(projectExpr(expr = vr("x"), asAlias = defnidReg("target"))),
            from = scan(
                graphMatch(
                    expr = vr("my_graph"),
                    gpmlPattern = gpmlPattern(
                        patterns = listOf(
                            graphMatchPattern(
                                parts = listOf(
                                    node(
                                        prefilter = null,
                                        variable = defnidReg("x"),
                                        label = graphLabelName(defnidReg("Label"))
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            where = eq(
                listOf(
                    path(vr("x"), pathExpr(lit(ionString("has_data")), regular())),
                    lit(ionBool(true))
                )
            )
        )
    }

    @Test
    fun allEdges() = assertExpression(
        "SELECT 1 FROM g MATCH -[]-> ",
    ) {
        select(
            project = projectList(projectExpr(lit(ionInt(1)))),
            from = scan(
                graphMatch(
                    expr = vr("g"),
                    gpmlPattern = gpmlPattern(
                        patterns = listOf(
                            graphMatchPattern(
                                parts = listOf(
                                    edge(
                                        direction = edgeRight(),
                                        quantifier = null,
                                        prefilter = null,
                                        variable = null,
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            where = null
        )
    }

    @Test
    fun allEdgesAllNodes() = assertExpression(
        "SELECT 1 FROM (g MATCH -[]->, ())",
    ) {
        select(
            project = projectList(projectExpr(lit(ionInt(1)))),
            from = scan(
                graphMatch(
                    expr = vr("g"),
                    gpmlPattern = gpmlPattern(
                        patterns = listOf(
                            graphMatchPattern(
                                parts = listOf(
                                    edge(
                                        direction = edgeRight(),
                                        quantifier = null,
                                        prefilter = null,
                                        variable = null,
                                    ),
                                )
                            ),
                            graphMatchPattern(
                                parts = listOf(
                                    node(
                                        prefilter = null,
                                        variable = null,
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            where = null
        )
    }

    @Test
    fun allNodesAllEdges() = assertExpression(
        "SELECT 1 FROM (g MATCH (), -[]-> )",
    ) {
        select(
            project = projectList(projectExpr(lit(ionInt(1)))),
            from = scan(
                graphMatch(
                    expr = vr("g"),
                    gpmlPattern = gpmlPattern(
                        patterns = listOf(
                            graphMatchPattern(
                                parts = listOf(
                                    node(
                                        prefilter = null,
                                        variable = null,
                                    )
                                )
                            ),
                            graphMatchPattern(
                                parts = listOf(
                                    edge(
                                        direction = edgeRight(),
                                        quantifier = null,
                                        prefilter = null,
                                        variable = null,
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            where = null
        )
    }

    val simpleGraphAST =
        { direction: PartiqlAst.GraphMatchDirection, quantifier: PartiqlAst.GraphMatchQuantifier?,
            variable: PartiqlAst.Defnid?, label: PartiqlAst.Defnid? ->
            PartiqlAst.build {
                select(
                    project = projectList(projectExpr(vr("a")), projectExpr(vr("b"))),
                    from = scan(
                        graphMatch(
                            expr = vr("g"),
                            gpmlPattern = gpmlPattern(
                                patterns = listOf(
                                    graphMatchPattern(
                                        quantifier = null,
                                        parts = listOf(
                                            node(
                                                prefilter = null,
                                                variable = defnidReg("a"),
                                                label = graphLabelName(defnidReg("A"))
                                            ),
                                            edge(
                                                direction = direction,
                                                quantifier = quantifier,
                                                prefilter = null,
                                                variable = variable,
                                                label = label?.let { graphLabelName(it) }
                                            ),
                                            node(
                                                prefilter = null,
                                                variable = defnidReg("b"),
                                                label = graphLabelName(defnidReg("B"))
                                            ),
                                        )
                                    )
                                )
                            )
                        )
                    ),
                    where = null
                )
            }
        }

    @Test
    fun rightDirected() = assertExpression(
        "SELECT a,b FROM g MATCH (a:A) -[e:E]-> (b:B)",
    ) {
        simpleGraphAST(edgeRight(), null, defnidReg("e"), defnidReg("E"))
    }

    @Test
    fun rightDirectedAbbreviated() = assertExpression(
        "SELECT a,b FROM g MATCH (a:A) -> (b:B)",
    ) {
        simpleGraphAST(edgeRight(), null, null, null)
    }

    @Test
    fun leftDirected() = assertExpression(
        "SELECT a,b FROM g MATCH (a:A) <-[e:E]- (b:B)",
    ) {
        simpleGraphAST(edgeLeft(), null, defnidReg("e"), defnidReg("E"))
    }

    @Test
    fun leftDirectedAbbreviated() = assertExpression(
        "SELECT a,b FROM g MATCH (a:A) <- (b:B)",
    ) {
        simpleGraphAST(edgeLeft(), null, null, null)
    }

    @Test
    fun undirected() = assertExpression(
        "SELECT a,b FROM g MATCH (a:A) ~[e:E]~ (b:B)",
    ) {
        simpleGraphAST(edgeUndirected(), null, defnidReg("e"), defnidReg("E"))
    }

    @Test
    fun undirectedAbbreviated() = assertExpression(
        "SELECT a,b FROM g MATCH (a:A) ~ (b:B)",
    ) {
        simpleGraphAST(edgeUndirected(), null, null, null)
    }

    @Test
    fun rightOrUnDirected() = assertExpression(
        "SELECT a,b FROM g MATCH (a:A) ~[e:E]~> (b:B)",
    ) {
        simpleGraphAST(edgeUndirectedOrRight(), null, defnidReg("e"), defnidReg("E"))
    }

    @Test
    fun rightOrUnDirectedAbbreviated() = assertExpression(
        "SELECT a,b FROM g MATCH (a:A) ~> (b:B)",
    ) {
        simpleGraphAST(edgeUndirectedOrRight(), null, null, null)
    }

    @Test
    fun leftOrUnDirected() = assertExpression(
        "SELECT a,b FROM g MATCH (a:A) <~[e:E]~ (b:B)",
    ) {
        simpleGraphAST(edgeLeftOrUndirected(), null, defnidReg("e"), defnidReg("E"))
    }

    @Test
    fun leftOrUnDirectedAbbreviated() = assertExpression(
        "SELECT a,b FROM g MATCH (a:A) <~ (b:B)",
    ) {
        simpleGraphAST(edgeLeftOrUndirected(), null, null, null)
    }

    @Test
    fun leftOrRight() = assertExpression(
        "SELECT a,b FROM g MATCH (a:A) <-[e:E]-> (b:B)",
    ) {
        simpleGraphAST(edgeLeftOrRight(), null, defnidReg("e"), defnidReg("E"))
    }

    @Test
    fun leftOrRightAbbreviated() = assertExpression(
        "SELECT a,b FROM g MATCH (a:A) <-> (b:B)",
    ) {
        simpleGraphAST(edgeLeftOrRight(), null, null, null)
    }

    @Test
    fun leftOrRightOrUndirected() = assertExpression(
        "SELECT a,b FROM g MATCH (a:A) -[e:E]- (b:B)",
    ) {
        simpleGraphAST(edgeLeftOrUndirectedOrRight(), null, defnidReg("e"), defnidReg("E"))
    }

    @Test
    fun leftOrRightOrUndirectedAbbreviated() = assertExpression(
        "SELECT a,b FROM g MATCH (a:A) - (b:B)",
    ) {
        simpleGraphAST(edgeLeftOrUndirectedOrRight(), null, null, null)
    }

    @Test
    fun quantifierStar() = assertExpression(
        "SELECT a,b FROM g MATCH (a:A)-[:edge]->*(b:B)",
    ) {
        simpleGraphAST(edgeRight(), graphMatchQuantifier(lower = 0, upper = null), null, defnidReg("edge"))
    }

    @Test
    fun quantifierPlus() = assertExpression(
        "SELECT a,b FROM g MATCH (a:A)<-[:edge]-+(b:B)",
    ) {
        simpleGraphAST(edgeLeft(), graphMatchQuantifier(lower = 1, upper = null), null, defnidReg("edge"))
    }

    @Test
    fun quantifierM() = assertExpression(
        "SELECT a,b FROM g MATCH (a:A)~[:edge]~{5,}(b:B)",
    ) {
        simpleGraphAST(edgeUndirected(), graphMatchQuantifier(lower = 5, upper = null), null, defnidReg("edge"))
    }

    @Test
    fun quantifierMN() = assertExpression(
        "SELECT a,b FROM g MATCH (a:A)-[e:edge]-{2,6}(b:B)",
    ) {
        simpleGraphAST(edgeLeftOrUndirectedOrRight(), graphMatchQuantifier(lower = 2, upper = 6), defnidReg("e"), defnidReg("edge"))
    }

    @Test
    fun quantifierAbbreviatedStar() = assertExpression(
        "SELECT a,b FROM g MATCH (a:A)->*(b:B)",
    ) {
        simpleGraphAST(edgeRight(), graphMatchQuantifier(lower = 0, upper = null), null, null)
    }

    @Test
    fun quantifierAbbreviatedPlus() = assertExpression(
        "SELECT a,b FROM g MATCH (a:A)<-+(b:B)",
    ) {
        simpleGraphAST(edgeLeft(), graphMatchQuantifier(lower = 1, upper = null), null, null)
    }

    @Test
    fun quantifierAbbreviatedM() = assertExpression(
        "SELECT a,b FROM g MATCH (a:A)~{5,}(b:B)",
    ) {
        simpleGraphAST(edgeUndirected(), graphMatchQuantifier(lower = 5, upper = null), null, null)
    }

    @Test
    fun quantifierAbbreviatedMN() = assertExpression(
        "SELECT a,b FROM g MATCH (a:A)-{2,6}(b:B)",
    ) {
        simpleGraphAST(edgeLeftOrUndirectedOrRight(), graphMatchQuantifier(lower = 2, upper = 6), null, null)
    }

    @Test
    fun singleEdgeMatch() = assertExpression(
        "SELECT the_a.name AS src, the_b.name AS dest FROM my_graph MATCH (the_a:a) -[the_y:y]-> (the_b:b) WHERE the_y.score > 10",
    ) {
        select(
            project = projectList(
                projectExpr(
                    expr = path(vr("the_a"), pathExpr(lit(ionString("name")), regular())),
                    asAlias = defnidReg("src")
                ),
                projectExpr(
                    expr = path(vr("the_b"), pathExpr(lit(ionString("name")), regular())),
                    asAlias = defnidReg("dest")
                )
            ),
            from = scan(
                graphMatch(
                    expr = vr("my_graph"),
                    gpmlPattern = gpmlPattern(
                        patterns = listOf(
                            graphMatchPattern(
                                parts = listOf(
                                    node(
                                        prefilter = null,
                                        variable = defnidReg("the_a"),
                                        label = graphLabelName(defnidReg("a"))
                                    ),
                                    edge(
                                        direction = edgeRight(),
                                        quantifier = null,
                                        prefilter = null,
                                        variable = defnidReg("the_y"),
                                        label = graphLabelName(defnidReg("y"))
                                    ),
                                    node(
                                        prefilter = null,
                                        variable = defnidReg("the_b"),
                                        label = graphLabelName(defnidReg("b"))
                                    ),
                                )
                            )
                        )
                    )
                )
            ),
            where = gt(
                listOf(
                    path(vr("the_y"), pathExpr(lit(ionString("score")), regular())),
                    lit(ionInt(10))
                )
            )
        )
    }

    @Test
    fun twoHopTriples() = assertExpression(
        "SELECT a,b FROM (g MATCH (a) -[:has]-> (x), (x)-[:contains]->(b))",
    ) {
        select(
            project = projectList(
                projectExpr(expr = vr("a")),
                projectExpr(expr = vr("b"))
            ),
            from = scan(
                graphMatch(
                    expr = vr("g"),
                    gpmlPattern = gpmlPattern(
                        patterns = listOf(
                            graphMatchPattern(
                                parts = listOf(
                                    node(
                                        prefilter = null,
                                        variable = defnidReg("a"),
                                    ),
                                    edge(
                                        direction = edgeRight(),
                                        quantifier = null,
                                        prefilter = null,
                                        variable = null,
                                        label = graphLabelName(defnidReg("has"))
                                    ),
                                    node(
                                        prefilter = null,
                                        variable = defnidReg("x"),
                                    ),
                                )
                            ),
                            graphMatchPattern(
                                parts = listOf(
                                    node(
                                        prefilter = null,
                                        variable = defnidReg("x"),
                                    ),
                                    edge(
                                        direction = edgeRight(),
                                        quantifier = null,
                                        prefilter = null,
                                        variable = null,
                                        label = graphLabelName(defnidReg("contains"))
                                    ),
                                    node(
                                        prefilter = null,
                                        variable = defnidReg("b"),
                                    ),
                                )
                            )
                        )
                    )
                )
            )
        )
    }

    @Test
    fun twoHopPattern() = assertExpression(
        "SELECT a,b FROM g MATCH (a)-[:has]->()-[:contains]->(b)",
    ) {
        select(
            project = projectList(
                projectExpr(expr = vr("a")),
                projectExpr(expr = vr("b"))
            ),
            from = scan(
                graphMatch(
                    expr = vr("g"),
                    gpmlPattern = gpmlPattern(
                        patterns = listOf(
                            graphMatchPattern(
                                parts = listOf(
                                    node(
                                        prefilter = null,
                                        variable = defnidReg("a"),
                                    ),
                                    edge(
                                        direction = edgeRight(),
                                        quantifier = null,
                                        prefilter = null,
                                        variable = null,
                                        label = graphLabelName(defnidReg("has"))
                                    ),
                                    node(
                                        prefilter = null,
                                        variable = null,
                                    ),
                                    edge(
                                        direction = edgeRight(),
                                        quantifier = null,
                                        prefilter = null,
                                        variable = null,
                                        label = graphLabelName(defnidReg("contains"))
                                    ),
                                    node(
                                        prefilter = null,
                                        variable = defnidReg("b"),
                                    ),
                                )
                            )
                        )
                    )
                )
            )
        )
    }

    @Test
    fun pathVariable() = assertExpression(
        "SELECT a,b FROM g MATCH p = (a:A) -[e:E]-> (b:B)",
    ) {
        PartiqlAst.build {
            select(
                project = projectList(projectExpr(vr("a")), projectExpr(vr("b"))),
                from = scan(
                    graphMatch(
                        expr = vr("g"),
                        gpmlPattern = gpmlPattern(
                            patterns = listOf(
                                graphMatchPattern(
                                    variable = defnidReg("p"),
                                    parts = listOf(
                                        node(
                                            variable = defnidReg("a"),
                                            label = graphLabelName(defnidReg("A"))
                                        ),
                                        edge(
                                            direction = edgeRight(),
                                            variable = defnidReg("e"),
                                            label = graphLabelName(defnidReg("E"))
                                        ),
                                        node(
                                            variable = defnidReg("b"),
                                            label = graphLabelName(defnidReg("B"))
                                        ),
                                    )
                                )
                            )
                        )
                    )
                ),
                where = null
            )
        }
    }

    @Test
    fun parenthesizedPatternWithFilter() = assertExpression(
        "SELECT a,b FROM g MATCH [(a:A)-[e:Edge]->(b:A) WHERE a.owner=b.owner]{2,5}",
    ) {
        PartiqlAst.build {
            select(
                project = projectList(projectExpr(vr("a")), projectExpr(vr("b"))),
                from = scan(
                    graphMatch(
                        expr = vr("g"),
                        gpmlPattern = gpmlPattern(
                            patterns = listOf(
                                graphMatchPattern(
                                    parts = listOf(
                                        pattern(
                                            graphMatchPattern(
                                                prefilter = eq(
                                                    path(vr("a"), pathExpr(lit(ionString("owner")), regular())),
                                                    path(vr("b"), pathExpr(lit(ionString("owner")), regular()))
                                                ),
                                                quantifier = graphMatchQuantifier(lower = 2, upper = 5),
                                                parts = listOf(
                                                    node(
                                                        variable = defnidReg("a"),
                                                        label = graphLabelName(defnidReg("A"))
                                                    ),
                                                    edge(
                                                        direction = edgeRight(),
                                                        variable = defnidReg("e"),
                                                        label = graphLabelName(defnidReg("Edge"))
                                                    ),
                                                    node(
                                                        variable = defnidReg("b"),
                                                        label = graphLabelName(defnidReg("A"))
                                                    ),
                                                ),
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                ),
                where = null
            )
        }
    }

    @Test
    fun parenthesizedEdgePattern() = assertExpression(
        "SELECT a,b FROM g MATCH pathVar = (a:A)[()-[e:Edge]->()]{1,3}(b:B)",
    ) {
        PartiqlAst.build {
            select(
                project = projectList(projectExpr(vr("a")), projectExpr(vr("b"))),
                from = scan(
                    graphMatch(
                        expr = vr("g"),
                        gpmlPattern = gpmlPattern(
                            patterns = listOf(
                                graphMatchPattern(
                                    variable = defnidReg("pathVar"),
                                    parts = listOf(
                                        node(
                                            variable = defnidReg("a"),
                                            label = graphLabelName(defnidReg("A"))
                                        ),
                                        pattern(
                                            graphMatchPattern(
                                                quantifier = graphMatchQuantifier(lower = 1, upper = 3),
                                                parts = listOf(
                                                    node(),
                                                    edge(
                                                        direction = edgeRight(),
                                                        variable = defnidReg("e"),
                                                        label = graphLabelName(defnidReg("Edge"))
                                                    ),
                                                    node(),
                                                )
                                            )
                                        ),
                                        node(
                                            variable = defnidReg("b"),
                                            label = graphLabelName(defnidReg("B"))
                                        ),
                                    )
                                )
                            )
                        )
                    )
                ),
                where = null
            )
        }
    }

    val parenthesizedEdgeStarAST = {
        PartiqlAst.build {
            select(
                project = projectList(projectExpr(vr("a")), projectExpr(vr("b"))),
                from = scan(
                    graphMatch(
                        expr = vr("g"),
                        gpmlPattern = gpmlPattern(
                            patterns = listOf(
                                graphMatchPattern(
                                    variable = defnidReg("pathVar"),
                                    parts = listOf(
                                        node(
                                            variable = defnidReg("a"),
                                            label = graphLabelName(defnidReg("A"))
                                        ),
                                        pattern(
                                            graphMatchPattern(
                                                quantifier = graphMatchQuantifier(lower = 0, upper = null),
                                                parts = listOf(
                                                    edge(
                                                        direction = edgeRight(),
                                                        variable = defnidReg("e"),
                                                        label = graphLabelName(defnidReg("Edge"))
                                                    ),
                                                )
                                            )
                                        ),
                                        node(
                                            variable = defnidReg("b"),
                                            label = graphLabelName(defnidReg("B"))
                                        ),
                                    )
                                )
                            )
                        )
                    )
                ),
                where = null
            )
        }
    }

    @Test
    fun squareParenthesizedEdgeStar() = assertExpression(
        "SELECT a,b FROM g MATCH pathVar = (a:A)[-[e:Edge]->]*(b:B)",
    ) {
        parenthesizedEdgeStarAST()
    }

    @Test
    fun roundParenthesizedEdgeStar() = assertExpression(
        "SELECT a,b FROM g MATCH pathVar = (a:A)(-[e:Edge]->)*(b:B)",
    ) {
        parenthesizedEdgeStarAST()
    }

    @Test
    fun prefilters() = assertExpression(
        "SELECT u as banCandidate FROM g MATCH (p:Post Where p.isFlagged = true) <-[:createdPost]- (u:Usr WHERE u.isBanned = false AND u.karma < 20) -[:createdComment]->(c:Comment WHERE c.isFlagged = true) WHERE p.title LIKE '%considered harmful%'",
    ) {
        PartiqlAst.build {
            select(
                project = projectList(projectExpr(vr("u"), asAlias = defnidReg("banCandidate"))),
                from = scan(
                    graphMatch(
                        expr = vr("g"),
                        gpmlPattern = gpmlPattern(
                            patterns = listOf(
                                graphMatchPattern(
                                    parts = listOf(
                                        node(
                                            variable = defnidReg("p"),
                                            label = graphLabelName(defnidReg("Post")),
                                            prefilter = eq(
                                                path(vr("p"), pathExpr(lit(ionString("isFlagged")), regular())),
                                                lit(ionBool(true))
                                            )
                                        ),
                                        edge(
                                            direction = edgeLeft(),
                                            label = graphLabelName(defnidReg("createdPost"))
                                        ),
                                        node(
                                            variable = defnidReg("u"),
                                            label = graphLabelName(defnidReg("Usr")),
                                            prefilter = and(
                                                eq(
                                                    path(
                                                        vr("u"),
                                                        pathExpr(lit(ionString("isBanned")), regular())
                                                    ),
                                                    lit(ionBool(false))
                                                ),
                                                lt(
                                                    path(vr("u"), pathExpr(lit(ionString("karma")), regular())),
                                                    lit(ionInt(20))
                                                )
                                            )
                                        ),
                                        edge(
                                            direction = edgeRight(),
                                            label = graphLabelName(defnidReg("createdComment"))
                                        ),
                                        node(
                                            variable = defnidReg("c"),
                                            label = graphLabelName(defnidReg("Comment")),
                                            prefilter =
                                            eq(
                                                path(vr("c"), pathExpr(lit(ionString("isFlagged")), regular())),
                                                lit(ionBool(true))
                                            )
                                        ),
                                    ),
                                )
                            )
                        )
                    )
                ),
                where = like(
                    value = path(vr("p"), pathExpr(lit(ionString("title")), regular())),
                    pattern = lit(ionString("%considered harmful%"))
                )
            )
        }
    }

    val restrictorAst = { restrictor: PartiqlAst.GraphMatchRestrictor ->
        PartiqlAst.build {
            select(
                project = projectList(projectExpr(vr("p"))),
                from = scan(
                    graphMatch(
                        expr = vr("g"),
                        gpmlPattern = gpmlPattern(
                            patterns = listOf(
                                graphMatchPattern(
                                    restrictor = restrictor,
                                    variable = defnidReg("p"),
                                    parts = listOf(
                                        node(
                                            variable = defnidReg("a"),
                                            prefilter =
                                            eq(
                                                path(vr("a"), pathExpr(lit(ionString("owner")), regular())),
                                                lit(ionString("Dave"))
                                            ),
                                        ),
                                        edge(
                                            direction = edgeRight(),
                                            variable = defnidReg("t"),
                                            label = graphLabelName(defnidReg("Transfer")),
                                            quantifier = graphMatchQuantifier(0)
                                        ),
                                        node(
                                            variable = defnidReg("b"),
                                            prefilter =
                                            eq(
                                                path(vr("b"), pathExpr(lit(ionString("owner")), regular())),
                                                lit(ionString("Aretha"))
                                            ),
                                        ),
                                    )
                                )
                            )
                        )
                    )
                ),
                where = null
            )
        }
    }

    @Test
    fun restrictorTrail() = assertExpression(
        "SELECT p FROM g MATCH TRAIL p = (a WHERE a.owner='Dave') -[t:Transfer]-> * (b WHERE b.owner='Aretha')",
    ) {
        restrictorAst(restrictorTrail())
    }

    @Test
    fun restrictorAcyclic() = assertExpression(
        "SELECT p FROM g MATCH ACYCLIC p = (a WHERE a.owner='Dave') -[t:Transfer]-> * (b WHERE b.owner='Aretha')",
    ) {
        restrictorAst(restrictorAcyclic())
    }

    @Test
    fun restrictorSimple() = assertExpression(
        "SELECT p FROM g MATCH SIMPLE p = (a WHERE a.owner='Dave') -[t:Transfer]-> * (b WHERE b.owner='Aretha')",
    ) {
        restrictorAst(restrictorSimple())
    }

    val selectorAST = { selector: PartiqlAst.GraphMatchSelector ->
        PartiqlAst.build {
            select(
                project = projectList(projectExpr(vr("p"))),
                from = scan(
                    graphMatch(
                        expr = vr("g"),
                        gpmlPattern = gpmlPattern(
                            selector = selector,
                            patterns = listOf(
                                graphMatchPattern(
                                    variable = defnidReg("p"),
                                    parts = listOf(
                                        node(
                                            variable = defnidReg("a"),
                                            prefilter =
                                            eq(
                                                path(vr("a"), pathExpr(lit(ionString("owner")), regular())),
                                                lit(ionString("Dave"))
                                            ),
                                        ),
                                        edge(
                                            direction = edgeRight(),
                                            variable = defnidReg("t"),
                                            label = graphLabelName(defnidReg("Transfer")),
                                            quantifier = graphMatchQuantifier(0)
                                        ),
                                        node(
                                            variable = defnidReg("b"),
                                            prefilter =
                                            eq(
                                                path(vr("b"), pathExpr(lit(ionString("owner")), regular())),
                                                lit(ionString("Aretha"))
                                            ),
                                        ),
                                    )
                                )
                            )
                        )
                    )
                ),
                where = null
            )
        }
    }

    @Test
    fun selectorAnyShortest() = assertExpression(
        "SELECT p FROM g MATCH ANY SHORTEST p = (a WHERE a.owner='Dave') -[t:Transfer]-> * (b WHERE b.owner='Aretha')",
    ) {
        selectorAST(selectorAnyShortest())
    }

    @Test
    fun selectorAllShortest() = assertExpression(
        "SELECT p FROM g MATCH All SHORTEST p = (a WHERE a.owner='Dave') -[t:Transfer]-> * (b WHERE b.owner='Aretha')",
    ) {
        selectorAST(selectorAllShortest())
    }

    @Test
    fun selectorAny() = assertExpression(
        "SELECT p FROM g MATCH ANY p = (a WHERE a.owner='Dave') -[t:Transfer]-> * (b WHERE b.owner='Aretha')",
    ) {
        selectorAST(selectorAny())
    }

    @Test
    fun selectorAnyK() = assertExpression(
        "SELECT p FROM g MATCH ANY 5 p = (a WHERE a.owner='Dave') -[t:Transfer]-> * (b WHERE b.owner='Aretha')",
    ) {
        selectorAST(selectorAnyK(5))
    }

    @Test
    fun selectorShortestK() = assertExpression(
        "SELECT p FROM g MATCH SHORTEST 5 p = (a WHERE a.owner='Dave') -[t:Transfer]-> * (b WHERE b.owner='Aretha')",
    ) {
        selectorAST(selectorShortestK(5))
    }

    @Test
    fun selectorShortestKGroup() = assertExpression(
        "SELECT p FROM g MATCH SHORTEST 5 GROUP p = (a WHERE a.owner='Dave') -[t:Transfer]-> * (b WHERE b.owner='Aretha')",
    ) {
        selectorAST(selectorShortestKGroup(5))
    }

    val joinedMatch = {
        val match = PartiqlAst.build {
            scan(
                graphMatch(
                    expr = vr("graph"),
                    gpmlPattern = gpmlPattern(
                        patterns = listOf(
                            graphMatchPattern(
                                parts = listOf(
                                    node(variable = defnidReg("a")),
                                    edge(direction = edgeRight()),
                                    node(variable = defnidReg("b")),
                                )
                            ),
                            graphMatchPattern(
                                parts = listOf(
                                    node(variable = defnidReg("a")),
                                    edge(direction = edgeRight()),
                                    node(variable = defnidReg("c")),
                                )
                            )
                        )
                    )
                )
            )
        }

        val t1 = PartiqlAst.build {
            scan(expr = vr("table1"), asAlias = defnidReg("t1"))
        }

        val t2 = PartiqlAst.build {
            scan(expr = vr("table2"), asAlias = defnidReg("t2"))
        }

        PartiqlAst.build {
            select(
                project = projectList(
                    projectExpr(vr("a")),
                    projectExpr(vr("b")),
                    projectExpr(vr("c")),
                    projectExpr(path(vr("t1"), pathExpr(lit(ionString("x")), regular())), defnidReg("x")),
                    projectExpr(path(vr("t2"), pathExpr(lit(ionString("y")), regular())), defnidReg("y"))
                ),
                from = join(
                    type = inner(),
                    left = join(
                        type = inner(),
                        left = match,
                        right = t1
                    ),
                    right = t2
                ),
                where = null
            )
        }
    }

    @Test
    fun matchAndJoinCommasParenthesized() {
        // fails because of the outer parentheses in ((a) -> (b), (a) -> (c)))
        assertFailsWith<ParserException> {
            assertExpression(
                "SELECT a,b,c, t1.x as x, t2.y as y FROM graph MATCH ((a) -> (b), (a) -> (c)), table1 as t1, table2 as t2",
            ) {
                joinedMatch()
            }
        }
    }

    @Test
    fun matchAndJoinCommas() {
        // fails because of the comma in the pattern and no parentheses like `(graph MATCH ...)`
        assertFailsWith<ParserException> {
            assertExpression(
                "SELECT a,b,c, t1.x as x, t2.y as y FROM graph MATCH (a) -> (b), (a) -> (c), table1 as t1, table2 as t2",
            ) {
                joinedMatch()
            }
        }
    }

    @Test
    fun matchAndJoinCommasParenthesized_outerParens() {
        // fails because of the outer parentheses in ((a) -> (b), (a) -> (c)))
        assertFailsWith<ParserException> {
            assertExpression(
                "SELECT a,b,c, t1.x as x, t2.y as y FROM (graph MATCH ((a) -> (b), (a) -> (c))), table1 as t1, table2 as t2",
            ) {
                joinedMatch()
            }
        }
    }

    @Test
    fun matchAndJoinCommas_outerParens() {
        assertExpression(
            "SELECT a,b,c, t1.x as x, t2.y as y FROM (graph MATCH (a) -> (b), (a) -> (c)), table1 as t1, table2 as t2",
        ) {
            joinedMatch()
        }
    }

    /** "SELECT x FROM g MATCH (x:$[spec])" */
    private fun astSelectNodeWithLabelSpec(spec: PartiqlAst.GraphLabelSpec) = PartiqlAst.build {
        select(
            project = projectList(projectExpr(expr = vr("x"))),
            from = scan(
                expr = graphMatch(
                    expr = vr("g"),
                    gpmlPattern = gpmlPattern(
                        patterns = listOf(
                            graphMatchPattern(
                                parts = listOf(
                                    node(
                                        prefilter = null,
                                        variable = defnidReg("x"),
                                        label = spec,
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
    }

    @Test
    fun labelSimpleNamed() = assertExpression(
        "SELECT x FROM g MATCH (x:A)"
    ) {
        astSelectNodeWithLabelSpec(spec = graphLabelName(defnidReg("A")))
    }

    @Test
    fun labelDisjunction() = assertExpression(
        "SELECT x FROM g MATCH (x:Label|OtherLabel)",
    ) {
        astSelectNodeWithLabelSpec(
            spec = graphLabelDisj(graphLabelName(defnidReg("Label")), graphLabelName(defnidReg("OtherLabel")))
        )
    }

    @Test
    fun labelConjunction() = assertExpression(
        "SELECT x FROM g MATCH (x:Label&OtherLabel)",
    ) {
        astSelectNodeWithLabelSpec(
            spec = graphLabelConj(graphLabelName(defnidReg("Label")), graphLabelName(defnidReg("OtherLabel")))
        )
    }

    @Test
    fun labelNegation() = assertExpression(
        "SELECT x FROM g MATCH (x:!Label)",
    ) {
        astSelectNodeWithLabelSpec(spec = graphLabelNegation(graphLabelName(defnidReg("Label"))))
    }

    @Test
    fun labelWildcard() = assertExpression(
        "SELECT x FROM g MATCH (x:%)",
    ) {
        astSelectNodeWithLabelSpec(spec = graphLabelWildcard())
    }

    private val astLabelCombo = PartiqlAst.build {
        astSelectNodeWithLabelSpec(
            spec = graphLabelDisj(
                graphLabelDisj(
                    graphLabelDisj(
                        graphLabelName(defnidReg("L1")),
                        graphLabelConj(graphLabelName(defnidReg("L2")), graphLabelName(defnidReg("L3")))
                    ),
                    graphLabelNegation(graphLabelName(defnidReg("L4")))
                ),
                graphLabelConj(graphLabelName(defnidReg("L5")), graphLabelWildcard())
            )
        )
    }
    @Test
    fun labelCombo() = assertExpression(
        "SELECT x FROM g MATCH (x: L1|L2&L3|!L4|(L5&%))",
    ) { astLabelCombo }

    @Test
    fun labelComboParens() = assertExpression(
        "SELECT x FROM g MATCH (x: ((L1 | (L2&L3)) | !L4) | (L5&%))",
    ) { astLabelCombo }

    /** (g MATCH <-[:$[spec]]-> ) */
    private fun astMatchEdgeWithLabelSpec(spec: PartiqlAst.GraphLabelSpec) = PartiqlAst.build {
        graphMatch(
            expr = vr("g"),
            gpmlPattern = gpmlPattern(
                patterns = listOf(
                    graphMatchPattern(
                        parts = listOf(
                            edge(
                                direction = edgeLeftOrRight(),
                                prefilter = null,
                                variable = null,
                                label = spec,
                            )
                        )
                    )
                )
            )
        )
    }

    @Test
    fun edgeLabelSimpleNamed() = assertExpression(
        "(g MATCH <-[:City]->)"
    ) {
        astMatchEdgeWithLabelSpec(graphLabelName(defnidReg("City")))
    }

    @Test
    fun edgeLabelOrAnd() = assertExpression(
        "(g MATCH <-[ : Country | City & Sovereign ]->)"
    ) {
        astMatchEdgeWithLabelSpec(
            graphLabelDisj(
                graphLabelName(defnidReg("Country")),
                graphLabelConj(graphLabelName(defnidReg("City")), graphLabelName(defnidReg("Sovereign")))
            )
        )
    }

    @Test
    fun edgeLabelUnlabeled() = assertExpression(
        "(g MATCH <-[:!%]->)"
    ) {
        astMatchEdgeWithLabelSpec(graphLabelNegation(graphLabelWildcard()))
    }

    // TODO group variables (e.g., `MATCH ... WHERE SUM()...`)
    // TODO union & multiset (e.g., `MATCH (a:Label) | (a:Label2)` , `MATCH (a:Label) |+| (a:Label2)`
    // TODO conditional variables
    // TODO graphical predicates (i.e., `IS DIRECTED`, `IS SOURCE OF`, `IS DESTINATION OF`, `SAME`, `ALL DIFFERENT`)
    // TODO restrictors & selectors (i.e., `TRAIL`|`ACYCLIC`|`SIMPLE`  & ANY SHORTEST, ALL SHORTEST, ANY, ANY k, SHORTEST k, SHORTEST k GROUP)
    // TODO selector filters
}
