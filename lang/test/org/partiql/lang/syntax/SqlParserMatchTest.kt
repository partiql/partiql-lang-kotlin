package org.partiql.lang.syntax

import com.amazon.ionelement.api.ionBool
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionString
import org.junit.Ignore
import org.junit.Test
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.id
import kotlin.test.assertFailsWith

class SqlParserMatchTest : SqlParserTestBase() {
    @Test
    fun allNodesNoLabel() = assertExpressionNoRoundTrip(
        "SELECT 1 FROM my_graph MATCH ()"
    ) {
        select(
            project = projectList(projectExpr(lit(ionInt(1)))),
            from = graphMatch(
                expr = id("my_graph"),
                graphExpr = graphMatchExpr(
                    patterns = listOf(
                        graphMatchPattern(
                            parts = listOf(
                                node(
                                    prefilter = null,
                                    variable = null,
                                    label = listOf()
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
    fun allNodesNoLabelFilter() = assertExpressionNoRoundTrip(
        "SELECT 1 FROM my_graph MATCH () WHERE contains_value('1')",
    ) {
        select(
            project = projectList(projectExpr(lit(ionInt(1)))),
            from = graphMatch(
                expr = id("my_graph"),
                graphExpr = graphMatchExpr(
                    patterns = listOf(
                        graphMatchPattern(
                            parts = listOf(
                                node(
                                    prefilter = null,
                                    variable = null,
                                    label = listOf()
                                )
                            )
                        )
                    )
                )
            ),
            where = call(funcName = "contains_value", args = listOf(lit(ionString("1"))))
        )
    }

    @Test
    fun allNodes() = assertExpressionNoRoundTrip(
        "SELECT x.info AS info FROM my_graph MATCH (x) WHERE x.name LIKE 'foo'",
    ) {
        select(
            project = projectList(
                projectExpr(
                    expr = path(id("x"), pathExpr(lit(ionString("info")), caseInsensitive())),
                    asAlias = "info"
                )
            ),
            from = graphMatch(
                expr = id("my_graph"),
                graphExpr = graphMatchExpr(
                    patterns = listOf(
                        graphMatchPattern(
                            parts = listOf(
                                node(
                                    prefilter = null,
                                    variable = "x",
                                    label = listOf()
                                )
                            )
                        )
                    )
                )
            ),
            where = like(
                value = path(id("x"), pathExpr(lit(ionString("name")), caseInsensitive())),
                pattern = lit(ionString("foo"))
            )
        )
    }

    @Test
    fun labelledNodes() = assertExpressionNoRoundTrip(
        "SELECT x AS target FROM my_graph MATCH (x:Label) WHERE x.has_data = true",
    ) {
        select(
            project = projectList(projectExpr(expr = id("x"), asAlias = "target")),
            from = graphMatch(
                expr = id("my_graph"),
                graphExpr = graphMatchExpr(
                    patterns = listOf(
                        graphMatchPattern(
                            parts = listOf(
                                node(
                                    prefilter = null,
                                    variable = "x",
                                    label = listOf("Label")
                                )
                            )
                        )
                    )
                )
            ),
            where = eq(
                listOf(
                    path(id("x"), pathExpr(lit(ionString("has_data")), caseInsensitive())),
                    lit(ionBool(true))
                )
            )
        )
    }

    @Test
    fun allEdges() = assertExpressionNoRoundTrip(
        "SELECT 1 FROM g MATCH -[]-> ",
    ) {
        select(
            project = projectList(projectExpr(lit(ionInt(1)))),
            from = graphMatch(
                expr = id("g"),
                graphExpr = graphMatchExpr(
                    patterns = listOf(
                        graphMatchPattern(
                            parts = listOf(
                                edge(
                                    direction = edgeRight(),
                                    quantifier = null,
                                    prefilter = null,
                                    variable = null,
                                    label = listOf()
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
        { direction: PartiqlAst.GraphMatchDirection, quantifier: PartiqlAst.GraphMatchQuantifier?, variable: String?, label: List<String>? ->
            PartiqlAst.build {
                select(
                    project = projectList(projectExpr(id("a")), projectExpr(id("b"))),
                    from = graphMatch(
                        expr = id("g"),
                        graphExpr = graphMatchExpr(
                            patterns = listOf(
                                graphMatchPattern(
                                    quantifier = null,
                                    parts = listOf(
                                        node(
                                            prefilter = null,
                                            variable = "a",
                                            label = listOf("A")
                                        ),
                                        edge(
                                            direction = direction,
                                            quantifier = quantifier,
                                            prefilter = null,
                                            variable = variable,
                                            label = label ?: emptyList()
                                        ),
                                        node(
                                            prefilter = null,
                                            variable = "b",
                                            label = listOf("B")
                                        ),
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
    fun rightDirected() = assertExpressionNoRoundTrip(
        "SELECT a,b FROM g MATCH (a:A) -[e:E]-> (b:B)",
    ) {
        simpleGraphAST(edgeRight(), null, "e", listOf("E"))
    }

    @Test
    fun rightDirectedAbbreviated() = assertExpressionNoRoundTrip(
        "SELECT a,b FROM g MATCH (a:A) -> (b:B)",
    ) {
        simpleGraphAST(edgeRight(), null, null, null)
    }

    @Test
    fun leftDirected() = assertExpressionNoRoundTrip(
        "SELECT a,b FROM g MATCH (a:A) <-[e:E]- (b:B)",
    ) {
        simpleGraphAST(edgeLeft(), null, "e", listOf("E"))
    }

    @Test
    fun leftDirectedAbbreviated() = assertExpressionNoRoundTrip(
        "SELECT a,b FROM g MATCH (a:A) <- (b:B)",
    ) {
        simpleGraphAST(edgeLeft(), null, null, null)
    }

    @Test
    fun undirected() = assertExpressionNoRoundTrip(
        "SELECT a,b FROM g MATCH (a:A) ~[e:E]~ (b:B)",
    ) {
        simpleGraphAST(edgeUndirected(), null, "e", listOf("E"))
    }

    @Test
    fun undirectedAbbreviated() = assertExpressionNoRoundTrip(
        "SELECT a,b FROM g MATCH (a:A) ~ (b:B)",
    ) {
        simpleGraphAST(edgeUndirected(), null, null, null)
    }

    @Test
    fun rightOrUnDirected() = assertExpressionNoRoundTrip(
        "SELECT a,b FROM g MATCH (a:A) ~[e:E]~> (b:B)",
    ) {
        simpleGraphAST(edgeUndirectedOrRight(), null, "e", listOf("E"))
    }

    @Test
    fun rightOrUnDirectedAbbreviated() = assertExpressionNoRoundTrip(
        "SELECT a,b FROM g MATCH (a:A) ~> (b:B)",
    ) {
        simpleGraphAST(edgeUndirectedOrRight(), null, null, null)
    }

    @Test
    fun leftOrUnDirected() = assertExpressionNoRoundTrip(
        "SELECT a,b FROM g MATCH (a:A) <~[e:E]~ (b:B)",
    ) {
        simpleGraphAST(edgeLeftOrUndirected(), null, "e", listOf("E"))
    }

    @Test
    fun leftOrUnDirectedAbbreviated() = assertExpressionNoRoundTrip(
        "SELECT a,b FROM g MATCH (a:A) <~ (b:B)",
    ) {
        simpleGraphAST(edgeLeftOrUndirected(), null, null, null)
    }

    @Test
    fun leftOrRight() = assertExpressionNoRoundTrip(
        "SELECT a,b FROM g MATCH (a:A) <-[e:E]-> (b:B)",
    ) {
        simpleGraphAST(edgeLeftOrRight(), null, "e", listOf("E"))
    }

    @Test
    fun leftOrRightAbbreviated() = assertExpressionNoRoundTrip(
        "SELECT a,b FROM g MATCH (a:A) <-> (b:B)",
    ) {
        simpleGraphAST(edgeLeftOrRight(), null, null, null)
    }

    @Test
    fun leftOrRightOrUndirected() = assertExpressionNoRoundTrip(
        "SELECT a,b FROM g MATCH (a:A) -[e:E]- (b:B)",
    ) {
        simpleGraphAST(edgeLeftOrUndirectedOrRight(), null, "e", listOf("E"))
    }

    @Test
    fun leftOrRightOrUndirectedAbbreviated() = assertExpressionNoRoundTrip(
        "SELECT a,b FROM g MATCH (a:A) - (b:B)",
    ) {
        simpleGraphAST(edgeLeftOrUndirectedOrRight(), null, null, null)
    }

    @Test
    fun quantifierStar() = assertExpressionNoRoundTrip(
        "SELECT a,b FROM g MATCH (a:A)-[:edge]->*(b:B)",
    ) {
        simpleGraphAST(edgeRight(), graphMatchQuantifier(lower = 0, upper = null), null, listOf("edge"))
    }

    @Test
    fun quantifierPlus() = assertExpressionNoRoundTrip(
        "SELECT a,b FROM g MATCH (a:A)<-[:edge]-+(b:B)",
    ) {
        simpleGraphAST(edgeLeft(), graphMatchQuantifier(lower = 1, upper = null), null, listOf("edge"))
    }

    @Test
    fun quantifierM() = assertExpressionNoRoundTrip(
        "SELECT a,b FROM g MATCH (a:A)~[:edge]~{5,}(b:B)",
    ) {
        simpleGraphAST(edgeUndirected(), graphMatchQuantifier(lower = 5, upper = null), null, listOf("edge"))
    }

    @Test
    fun quantifierMN() = assertExpressionNoRoundTrip(
        "SELECT a,b FROM g MATCH (a:A)-[e:edge]-{2,6}(b:B)",
    ) {
        simpleGraphAST(edgeLeftOrUndirectedOrRight(), graphMatchQuantifier(lower = 2, upper = 6), "e", listOf("edge"))
    }

    @Test
    fun quantifierAbbreviatedStar() = assertExpressionNoRoundTrip(
        "SELECT a,b FROM g MATCH (a:A)->*(b:B)",
    ) {
        simpleGraphAST(edgeRight(), graphMatchQuantifier(lower = 0, upper = null), null, null)
    }

    @Test
    fun quantifierAbbreviatedPlus() = assertExpressionNoRoundTrip(
        "SELECT a,b FROM g MATCH (a:A)<-+(b:B)",
    ) {
        simpleGraphAST(edgeLeft(), graphMatchQuantifier(lower = 1, upper = null), null, null)
    }

    @Test
    fun quantifierAbbreviatedM() = assertExpressionNoRoundTrip(
        "SELECT a,b FROM g MATCH (a:A)~{5,}(b:B)",
    ) {
        simpleGraphAST(edgeUndirected(), graphMatchQuantifier(lower = 5, upper = null), null, null)
    }

    @Test
    fun quantifierAbbreviatedMN() = assertExpressionNoRoundTrip(
        "SELECT a,b FROM g MATCH (a:A)-{2,6}(b:B)",
    ) {
        simpleGraphAST(edgeLeftOrUndirectedOrRight(), graphMatchQuantifier(lower = 2, upper = 6), null, null)
    }

    @Test
    fun singleEdgeMatch() = assertExpressionNoRoundTrip(
        "SELECT the_a.name AS src, the_b.name AS dest FROM my_graph MATCH (the_a:a) -[the_y:y]-> (the_b:b) WHERE the_y.score > 10",
    ) {
        select(
            project = projectList(
                projectExpr(
                    expr = path(id("the_a"), pathExpr(lit(ionString("name")), caseInsensitive())),
                    asAlias = "src"
                ),
                projectExpr(
                    expr = path(id("the_b"), pathExpr(lit(ionString("name")), caseInsensitive())),
                    asAlias = "dest"
                )
            ),
            from = graphMatch(
                expr = id("my_graph"),
                graphExpr = graphMatchExpr(
                    patterns = listOf(
                        graphMatchPattern(
                            parts = listOf(
                                node(
                                    prefilter = null,
                                    variable = "the_a",
                                    label = listOf("a")
                                ),
                                edge(
                                    direction = edgeRight(),
                                    quantifier = null,
                                    prefilter = null,
                                    variable = "the_y",
                                    label = listOf("y")
                                ),
                                node(
                                    prefilter = null,
                                    variable = "the_b",
                                    label = listOf("b")
                                ),
                            )
                        )
                    )
                )
            ),
            where = gt(
                listOf(
                    path(id("the_y"), pathExpr(lit(ionString("score")), caseInsensitive())),
                    lit(ionInt(10))
                )
            )
        )
    }

    @Test
    fun twoHopTriples() = assertExpressionNoRoundTrip(
        "SELECT a,b FROM g MATCH (a) -[:has]-> (x), (x)-[:contains]->(b)",
    ) {
        select(
            project = projectList(
                projectExpr(expr = id("a")),
                projectExpr(expr = id("b"))
            ),
            from = graphMatch(
                expr = id("g"),
                graphExpr = graphMatchExpr(
                    patterns = listOf(
                        graphMatchPattern(
                            parts = listOf(
                                node(
                                    prefilter = null,
                                    variable = "a",
                                    label = listOf()
                                ),
                                edge(
                                    direction = edgeRight(),
                                    quantifier = null,
                                    prefilter = null,
                                    variable = null,
                                    label = listOf("has")
                                ),
                                node(
                                    prefilter = null,
                                    variable = "x",
                                    label = listOf()
                                ),
                            )
                        ),
                        graphMatchPattern(
                            parts = listOf(
                                node(
                                    prefilter = null,
                                    variable = "x",
                                    label = listOf()
                                ),
                                edge(
                                    direction = edgeRight(),
                                    quantifier = null,
                                    prefilter = null,
                                    variable = null,
                                    label = listOf("contains")
                                ),
                                node(
                                    prefilter = null,
                                    variable = "b",
                                    label = listOf()
                                ),
                            )
                        )
                    )
                )
            )
        )
    }

    @Test
    fun twoHopPattern() = assertExpressionNoRoundTrip(
        "SELECT a,b FROM g MATCH (a)-[:has]->()-[:contains]->(b)",
    ) {
        select(
            project = projectList(
                projectExpr(expr = id("a")),
                projectExpr(expr = id("b"))
            ),
            from = graphMatch(
                expr = id("g"),
                graphExpr = graphMatchExpr(
                    patterns = listOf(
                        graphMatchPattern(
                            parts = listOf(
                                node(
                                    prefilter = null,
                                    variable = "a",
                                    label = listOf()
                                ),
                                edge(
                                    direction = edgeRight(),
                                    quantifier = null,
                                    prefilter = null,
                                    variable = null,
                                    label = listOf("has")
                                ),
                                node(
                                    prefilter = null,
                                    variable = null,
                                    label = listOf()
                                ),
                                edge(
                                    direction = edgeRight(),
                                    quantifier = null,
                                    prefilter = null,
                                    variable = null,
                                    label = listOf("contains")
                                ),
                                node(
                                    prefilter = null,
                                    variable = "b",
                                    label = listOf()
                                ),
                            )
                        )
                    )
                )
            )
        )
    }

    @Test
    fun pathVariable() = assertExpressionNoRoundTrip(
        "SELECT a,b FROM g MATCH p = (a:A) -[e:E]-> (b:B)",
    ) {
        PartiqlAst.build {
            select(
                project = projectList(projectExpr(id("a")), projectExpr(id("b"))),
                from = graphMatch(
                    expr = id("g"),
                    graphExpr = graphMatchExpr(
                        patterns = listOf(
                            graphMatchPattern(
                                variable = "p",
                                parts = listOf(
                                    node(
                                        variable = "a",
                                        label = listOf("A")
                                    ),
                                    edge(
                                        direction = edgeRight(),
                                        variable = "e",
                                        label = listOf("E")
                                    ),
                                    node(
                                        variable = "b",
                                        label = listOf("B")
                                    ),
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
    fun parenthesizedPatternWithFilter() = assertExpressionNoRoundTrip(
        "SELECT a,b FROM g MATCH [(a:A)-[e:Edge]->(b:A) WHERE a.owner=b.owner]{2,5}",
    ) {
        PartiqlAst.build {
            select(
                project = projectList(projectExpr(id("a")), projectExpr(id("b"))),
                from = graphMatch(
                    expr = id("g"),
                    graphExpr = graphMatchExpr(
                        patterns = listOf(
                            graphMatchPattern(
                                parts = listOf(
                                    pattern(
                                        graphMatchPattern(
                                            prefilter = eq(
                                                path(id("a"), pathExpr(lit(ionString("owner")), caseInsensitive())),
                                                path(id("b"), pathExpr(lit(ionString("owner")), caseInsensitive()))
                                            ),
                                            quantifier = graphMatchQuantifier(lower = 2, upper = 5),
                                            parts = listOf(
                                                node(
                                                    variable = "a",
                                                    label = listOf("A")
                                                ),
                                                edge(
                                                    direction = edgeRight(),
                                                    variable = "e",
                                                    label = listOf("Edge")
                                                ),
                                                node(
                                                    variable = "b",
                                                    label = listOf("A")
                                                ),
                                            ),
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
    fun parenthesizedEdgePattern() = assertExpressionNoRoundTrip(
        "SELECT a,b FROM g MATCH pathVar = (a:A)[()-[e:Edge]->()]{1,3}(b:B)",
    ) {
        PartiqlAst.build {
            select(
                project = projectList(projectExpr(id("a")), projectExpr(id("b"))),
                from = graphMatch(
                    expr = id("g"),
                    graphExpr = graphMatchExpr(
                        patterns = listOf(
                            graphMatchPattern(
                                variable = "pathVar",
                                parts = listOf(
                                    node(
                                        variable = "a",
                                        label = listOf("A")
                                    ),
                                    pattern(
                                        graphMatchPattern(
                                            quantifier = graphMatchQuantifier(lower = 1, upper = 3),
                                            parts = listOf(
                                                node(),
                                                edge(
                                                    direction = edgeRight(),
                                                    variable = "e",
                                                    label = listOf("Edge")
                                                ),
                                                node(),
                                            )
                                        )
                                    ),
                                    node(
                                        variable = "b",
                                        label = listOf("B")
                                    ),
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
                project = projectList(projectExpr(id("a")), projectExpr(id("b"))),
                from = graphMatch(
                    expr = id("g"),
                    graphExpr = graphMatchExpr(
                        patterns = listOf(
                            graphMatchPattern(
                                variable = "pathVar",
                                parts = listOf(
                                    node(
                                        variable = "a",
                                        label = listOf("A")
                                    ),
                                    pattern(
                                        graphMatchPattern(
                                            quantifier = graphMatchQuantifier(lower = 0, upper = null),
                                            parts = listOf(
                                                edge(
                                                    direction = edgeRight(),
                                                    variable = "e",
                                                    label = listOf("Edge")
                                                ),
                                            )
                                        )
                                    ),
                                    node(
                                        variable = "b",
                                        label = listOf("B")
                                    ),
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
    fun squareParenthesizedEdgeStar() = assertExpressionNoRoundTrip(
        "SELECT a,b FROM g MATCH pathVar = (a:A)[-[e:Edge]->]*(b:B)",
    ) {
        parenthesizedEdgeStarAST()
    }

    @Test
    fun roundParenthesizedEdgeStar() = assertExpressionNoRoundTrip(
        "SELECT a,b FROM g MATCH pathVar = (a:A)(-[e:Edge]->)*(b:B)",
    ) {
        parenthesizedEdgeStarAST()
    }

    @Test
    fun prefilters() = assertExpressionNoRoundTrip(
        "SELECT u as banCandidate FROM g MATCH (p:Post Where p.isFlagged = true) <-[:createdPost]- (u:User WHERE u.isBanned = false AND u.karma < 20) -[:createdComment]->(c:Comment WHERE c.isFlagged = true) WHERE p.title LIKE '%considered harmful%'",
    ) {
        PartiqlAst.build {
            select(
                project = projectList(projectExpr(id("u"), asAlias = "banCandidate")),
                from = graphMatch(
                    expr = id("g"),
                    graphExpr = graphMatchExpr(
                        patterns = listOf(
                            graphMatchPattern(
                                parts = listOf(
                                    node(
                                        variable = "p",
                                        label = listOf("Post"),
                                        prefilter = eq(
                                            path(id("p"), pathExpr(lit(ionString("isFlagged")), caseInsensitive())),
                                            lit(ionBool(true))
                                        )
                                    ),
                                    edge(
                                        direction = edgeLeft(),
                                        label = listOf("createdPost")
                                    ),
                                    node(
                                        variable = "u",
                                        label = listOf("User"),
                                        prefilter = and(
                                            eq(
                                                path(id("u"), pathExpr(lit(ionString("isBanned")), caseInsensitive())),
                                                lit(ionBool(false))
                                            ),
                                            lt(
                                                path(id("u"), pathExpr(lit(ionString("karma")), caseInsensitive())),
                                                lit(ionInt(20))
                                            )
                                        )
                                    ),
                                    edge(
                                        direction = edgeRight(),
                                        label = listOf("createdComment")
                                    ),
                                    node(
                                        variable = "c",
                                        label = listOf("Comment"),
                                        prefilter =
                                        eq(
                                            path(id("c"), pathExpr(lit(ionString("isFlagged")), caseInsensitive())),
                                            lit(ionBool(true))
                                        )
                                    ),
                                ),
                            )
                        )
                    )
                ),
                where = like(
                    value = path(id("p"), pathExpr(lit(ionString("title")), caseInsensitive())),
                    pattern = lit(ionString("%considered harmful%"))
                )
            )
        }
    }

    val restrictorAst = { restrictor: PartiqlAst.GraphMatchRestrictor ->
        PartiqlAst.build {
            select(
                project = projectList(projectExpr(id("p"))),
                from = graphMatch(
                    expr = id("g"),
                    graphExpr = graphMatchExpr(
                        patterns = listOf(
                            graphMatchPattern(
                                restrictor = restrictor,
                                variable = "p",
                                parts = listOf(
                                    node(
                                        variable = "a",
                                        prefilter =
                                        eq(
                                            path(id("a"), pathExpr(lit(ionString("owner")), caseInsensitive())),
                                            lit(ionString("Dave"))
                                        ),
                                    ),
                                    edge(
                                        direction = edgeRight(),
                                        variable = "t",
                                        label = listOf("Transfer"),
                                        quantifier = graphMatchQuantifier(0)
                                    ),
                                    node(
                                        variable = "b",
                                        prefilter =
                                        eq(
                                            path(id("b"), pathExpr(lit(ionString("owner")), caseInsensitive())),
                                            lit(ionString("Aretha"))
                                        ),
                                    ),
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
    fun restrictorTrail() = assertExpressionNoRoundTrip(
        "SELECT p FROM g MATCH TRAIL p = (a WHERE a.owner='Dave') -[t:Transfer]-> * (b WHERE b.owner='Aretha')",
    ) {
        restrictorAst(restrictorTrail())
    }

    @Test
    fun restrictorAcyclic() = assertExpressionNoRoundTrip(
        "SELECT p FROM g MATCH ACYCLIC p = (a WHERE a.owner='Dave') -[t:Transfer]-> * (b WHERE b.owner='Aretha')",
    ) {
        restrictorAst(restrictorAcyclic())
    }

    @Test
    fun restrictorSimple() = assertExpressionNoRoundTrip(
        "SELECT p FROM g MATCH SIMPLE p = (a WHERE a.owner='Dave') -[t:Transfer]-> * (b WHERE b.owner='Aretha')",
    ) {
        restrictorAst(restrictorSimple())
    }

    val selectorAST = { selector: PartiqlAst.GraphMatchSelector ->
        PartiqlAst.build {
            select(
                project = projectList(projectExpr(id("p"))),
                from = graphMatch(
                    expr = id("g"),
                    graphExpr = graphMatchExpr(
                        selector = selector,
                        patterns = listOf(
                            graphMatchPattern(
                                variable = "p",
                                parts = listOf(
                                    node(
                                        variable = "a",
                                        prefilter =
                                        eq(
                                            path(id("a"), pathExpr(lit(ionString("owner")), caseInsensitive())),
                                            lit(ionString("Dave"))
                                        ),
                                    ),
                                    edge(
                                        direction = edgeRight(),
                                        variable = "t",
                                        label = listOf("Transfer"),
                                        quantifier = graphMatchQuantifier(0)
                                    ),
                                    node(
                                        variable = "b",
                                        prefilter =
                                        eq(
                                            path(id("b"), pathExpr(lit(ionString("owner")), caseInsensitive())),
                                            lit(ionString("Aretha"))
                                        ),
                                    ),
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
    fun selectorAnyShortest() = assertExpressionNoRoundTrip(
        "SELECT p FROM g MATCH ANY SHORTEST p = (a WHERE a.owner='Dave') -[t:Transfer]-> * (b WHERE b.owner='Aretha')",
    ) {
        selectorAST(selectorAnyShortest())
    }

    @Test
    fun selectorAllShortest() = assertExpressionNoRoundTrip(
        "SELECT p FROM g MATCH All SHORTEST p = (a WHERE a.owner='Dave') -[t:Transfer]-> * (b WHERE b.owner='Aretha')",
    ) {
        selectorAST(selectorAllShortest())
    }

    @Test
    fun selectorAny() = assertExpressionNoRoundTrip(
        "SELECT p FROM g MATCH ANY p = (a WHERE a.owner='Dave') -[t:Transfer]-> * (b WHERE b.owner='Aretha')",
    ) {
        selectorAST(selectorAny())
    }

    @Test
    fun selectorAnyK() = assertExpressionNoRoundTrip(
        "SELECT p FROM g MATCH ANY 5 p = (a WHERE a.owner='Dave') -[t:Transfer]-> * (b WHERE b.owner='Aretha')",
    ) {
        selectorAST(selectorAnyK(5))
    }

    @Test
    fun selectorShortestK() = assertExpressionNoRoundTrip(
        "SELECT p FROM g MATCH SHORTEST 5 p = (a WHERE a.owner='Dave') -[t:Transfer]-> * (b WHERE b.owner='Aretha')",
    ) {
        selectorAST(selectorShortestK(5))
    }

    @Test
    fun selectorShortestKGroup() = assertExpressionNoRoundTrip(
        "SELECT p FROM g MATCH SHORTEST 5 GROUP p = (a WHERE a.owner='Dave') -[t:Transfer]-> * (b WHERE b.owner='Aretha')",
    ) {
        selectorAST(selectorShortestKGroup(5))
    }

    val joinedMatch = {
        val match = PartiqlAst.build {
            graphMatch(
                expr = id("graph"),
                graphExpr = graphMatchExpr(
                    patterns = listOf(
                        graphMatchPattern(
                            parts = listOf(
                                node(variable = "a"),
                                edge(direction = edgeRight()),
                                node(variable = "b"),
                            )
                        ),
                        graphMatchPattern(
                            parts = listOf(
                                node(variable = "a"),
                                edge(direction = edgeRight()),
                                node(variable = "c"),
                            )
                        )
                    )
                )
            )
        }

        val t1 = PartiqlAst.build {
            scan(expr = id("table1"), asAlias = "t1")
        }

        val t2 = PartiqlAst.build {
            scan(expr = id("table2"), asAlias = "t2")
        }

        PartiqlAst.build {
            select(
                project = projectList(
                    projectExpr(id("a")),
                    projectExpr(id("b")),
                    projectExpr(id("c")),
                    projectExpr(path(id("t1"), pathExpr(lit(ionString("x")), caseInsensitive())), "x"),
                    projectExpr(path(id("t2"), pathExpr(lit(ionString("y")), caseInsensitive())), "y")
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
    fun matchAndJoinCommasParenthesized() = assertExpressionNoRoundTrip(
        "SELECT a,b,c, t1.x as x, t2.y as y FROM graph MATCH ((a) -> (b), (a) -> (c)), table1 as t1, table2 as t2",
    ) {
        joinedMatch()
    }

    @Test
    fun matchAndJoinCommas() {
        assertFailsWith<ParserException> {
            assertExpressionNoRoundTrip(
                "SELECT a,b,c, t1.x as x, t2.y as y FROM graph MATCH (a) -> (b), (a) -> (c), table1 as t1, table2 as t2",
            ) {
                joinedMatch()
            }
        }
    }

    // TODO label combinators
    @Test
    @Ignore
    fun labelDisjunction() = assertExpressionNoRoundTrip(
        "SELECT x FROM g MATCH (x:Label|OtherLabel)",
    ) {
        TODO()
    }

    @Test
    @Ignore
    fun labelConjunction() = assertExpressionNoRoundTrip(
        "SELECT x FROM g MATCH (x:Label&OtherLabel)",
    ) {
        TODO()
    }

    @Test
    @Ignore
    fun labelNegation() = assertExpressionNoRoundTrip(
        "SELECT x FROM g MATCH (x:!Label)",
    ) {
        TODO()
    }

    @Test
    @Ignore
    fun labelWildcard() = assertExpressionNoRoundTrip(
        "SELECT x FROM g MATCH (x:%)",
    ) {
        TODO()
    }

    @Test
    @Ignore
    fun labelCombo() = assertExpressionNoRoundTrip(
        "SELECT x FROM g MATCH (x: L1|L2&L3|!L4|(L5&%)",
    ) {
        TODO()
    }

    // TODO group variables (e.g., `MATCH ... WHERE SUM()...`)
    // TODO union & multiset (e.g., `MATCH (a:Label) | (a:Label2)` , `MATCH (a:Label) |+| (a:Label2)`
    // TODO conditional variables
    // TODO graphical predicates (i.e., `IS DIRECTED`, `IS SOURCE OF`, `IS DESTINATION OF`, `SAME`, `ALL DIFFERENT`)
    // TODO restrictors & selectors (i.e., `TRAIL`|`ACYCLIC`|`SIMPLE`  & ANY SHORTEST, ALL SHORTEST, ANY, ANY k, SHORTEST k, SHORTEST k GROUP)
    // TODO selector filters
}
