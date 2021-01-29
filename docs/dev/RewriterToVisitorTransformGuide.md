# Rewriter Migration → PIG VisitorTransform 

## General Procedure

(migrating `FooRewriter` → `FooVisitorTransform`)

1. Make a copy of `FooRewriter` named `FooVisitorTransform` 
2. Similarly for the rewriter tests (if they exist) i.e. `FooRewriterTests` → `FooVisitorTransformTests`
3. Make the `FooVisitorTransform` have a base class of `PartiqlAst.VisitorTransform` rather than `AstRewriter`
4. Decide which code will require a migration to PIG
    1. Most commonly: anything involving `ExprNode` and ast.kt’s `MetaContainer`
5. For any call to override function `rewriteXXX`, find the equivalent `transformXXX` method in `partiql-domains.kt`
    1. First you need to find the `ExprNode` type’s equivalent `PartiqlAst` type. You can look at how the `ExprNode` is converted to a `PartiqlAst` in [`ExprNodeToStatement.kt`](https://github.com/partiql/partiql-lang-kotlin/blob/master/lang/src/org/partiql/lang/ast/ExprNodeToStatement.kt). Usually, this is a pretty simple 1:1 mapping, but there can be some ambiguities and differences between `ExprNode` and `PartiqlAst`. Some cases can be found in the “Common Conversions” section.
    2. After finding the type to convert, there should be a corresponding `transform` function in `partiql-domains.kt`. 
    3. You may need to add extension functions to do the same as an `ExprNode` but using `PartiqlAst`. Add these to `lang/domains/util.kt`
    4. Sometimes there are some trickier conversions. These cases are found in the sections below:
        1. Inner rewrites
            1. `innerRewriteSelect`
            2. `innerRewriteDataManipulation`
        2. `PartiqlAst.Expr` copying data
6. Write/translate tests, making sure they have a base class  `VisitorTransformTestBase`
7. Change the usages of `FooRewriter` to use `FooVisitorTransform`
8. Once tests are passing and meet the desired standard, ensure existing code isn’t broken by the changes (run `./gradlew clean build`)
9. Remove all parts of the old rewriter code:
    1. Delete unused imports
    2. Find all usages of the old rewriter (e.g. code, comments, kdoc) and replace them with the visitor transform version
    3. Delete the migrated rewriter (and its tests)
    4. (Optional but recommended) within `FooVisitorTransform` and its tests change usages of the term rewriter/rewritten to transformer/transformed
10. Rerun `./gradlew clean build` to make sure everything works

## Common Conversions

|ExprNode	|PartiqlAst	|ExprNode Rewriter	|PartiqlAst VisitorTransform	|Notes	|
|---	|---	|---	|---	|---	|
|VariableReference	|Expr.Id	|rewriteVariableReference	|transformExprId	|	|
|PathComponentExpr	|PathStep.PathExpr	|rewritePathComponent	|transformPathStepPathExpr	|	|
|Literal	|Expr.Lit	|rewriteLiteral	|transformExprLit	|	|
|NAryOp.CALL	|Expr.Call	|rewriteNAry, then case on NAryOp.CALL	|transformExprCall	|First arg in ExprNode is the function name, so removal of this isn't necessary for PartiqlAst	|
|FromSourceJoin	|FromSource.Join	|rewriteFromSourceJoin	|transformFromSourceJoin	|	|
|SelectProjectionPivot	|Projection.ProjectPivot	|rewriteSelectProjectionPivot	|transformProjectionProjectPivot	|	|
|SelectProjectionValue	|Projection.ProjectValue	|rewriteSelectProjectionValue	|transformProjectionProjectValue	|	|
|SelectProjectionList	|Projection.ProjectList	|rewriteSelectProjectionList	|transformProjectionProjectList	|	|
|FromSourceLet	|FromSource.Scan and FromSource.Unpivot	|rewriteFromSourceLet	|transformFromSourceScan and transformFromSourceUnpivot	|PartiqlAst does not have the supertype w/ Scan and Unpivot	|
|FromSourceExpr	|FromSource.Scan	|rewriteFromSourceExpr	|transformFromSourceScan	|	|
|ScopeQualifier.LEXICAL	|ScopeQualifier.LocalsFirst	|	|	|ScopeQualifier.UNQUALIFIED is the same	|

## Converting Metas

The constructor for metas is slightly different between ast’s MetaContainer and IonElement’s MetaContainer. Here is an example conversion:

```kotlin
metaContainerOf(UniqueNameMeta("foo"))
```

becomes 

```kotlin
 metaContainerOf(UniqueNameMeta.TAG to UniqueNameMeta("foo"))
```


Generally, you do not want to keep ast’s MetaContainer and have to use the conversion function ( [`toPartiQlMetaContainer`](https://github.com/partiql/partiql-lang-kotlin/blob/36fe55b6457f0dc82c02da6975358c01dd13aa3b/lang/src/org/partiql/lang/ast/StatementToExprNode.kt#L38))

To add a new meta to an IonElement MetaContainer, you can use the `withMeta(metaKey: String, metaValue: Any)`
function.

To add multiple metas, you will need to add two MetaContainers together:

```kotlin
metas = oldMetaContainer + metaContainerOf(newMeta)
```



## Inner Rewrite Query

In the rewriter code, you may encounter [`innerRewriteSelect`](https://github.com/partiql/partiql-lang-kotlin/blob/36fe55b6457f0dc82c02da6975358c01dd13aa3b/lang/src/org/partiql/lang/ast/passes/AstRewriterBase.kt#L151). This function calls the rewriter function for each clause of the SFW query. The direct equivalent in PIG is `transformExprSelect`.  In most cases you should be able to just call `transformExprSelect` directly or the base class’s transform, `super.transformExprSelect`.

However, currently there are some slight differences between the order of transforms in `innerRewriteSelect` and `transformExprSelect`. `innerRewriteSelect` follows the traversal order (SQL semantic order) that is

FROM → (FROM LET) → (WHERE) → (GROUP BY) → (HAVING) → PROJECTION → (LIMIT)

`transformExprSelect` transforms the clauses in the order they are written for a PartiQL query, that is

PROJECTION → FROM → (FROM LET) → (WHERE) → (GROUP BY) → (HAVING) → (LIMIT)

This slight difference can lead to some different behaviors when translating `innerRewriteSelect` (e.g. StaticTypeRewriter). So to completely solve this issue, you can have your VisitorTransform implement [`VisitorTransformBase`](https://github.com/partiql/partiql-lang-kotlin/blob/master/lang/src/org/partiql/lang/eval/visitors/VisitorTransformBase.kt) and call `transformExprSelectEvaluationOrder` rather than `transformExprSelect` to get the same behavior.

It's also worth noting that `innerRewriteSelect` and the visitor transform equivalent, `transformExprSelectEvaluationOrder`, can be used to avoid infinite recursion in the case of nested rewriter/transform instances.

```kotlin
fun transformExprSelectEvaluationOrder(node: PartiqlAst.Expr.Select): PartiqlAst.Expr {
    val from = transformExprSelect_from(node)
    val fromLet = transformExprSelect_fromLet(node)
    val where = transformExprSelect_where(node)
    val group = transformExprSelect_group(node)
    val having = transformExprSelect_having(node)
    val setq = transformExprSelect_setq(node)
    val project = transformExprSelect_project(node)
    val limit = transformExprSelect_limit(node)
    val metas = transformExprSelect_metas(node)
    return PartiqlAst.build {
        PartiqlAst.Expr.Select(
            setq = setq,
            project = project,
            from = from,
            fromLet = fromLet,
            where = where,
            group = group,
            having = having,
            limit = limit,
            metas = metas)
    }
}
```


This also applies to [`innerRewriteDataManipulation`](https://github.com/partiql/partiql-lang-kotlin/blob/36fe55b6457f0dc82c02da6975358c01dd13aa3b/lang/src/org/partiql/lang/ast/passes/AstRewriterBase.kt#L334), which has a slight different order than `transformStatementDml`.

(FROM) → (WHERE) → DML Operation  vs.  DML Operation → (FROM) → (WHERE)

To achieve the same behavior as the rewriter form, you can have your VisitorTransform implement [`VisitorTransformBase`](https://github.com/partiql/partiql-lang-kotlin/blob/master/lang/src/org/partiql/lang/eval/visitors/VisitorTransformBase.kt) and call `transformDataManipulationEvaluationOrder` rather than `transformStatementDml` to get the same behavior.

It's also worth noting that `innerRewriteDataManipulation` and the visitor transform equivalent, `transformDataManipulationEvaluationOrder`, can be used to avoid infinite recursion in the case of nested rewriter/transform instances.

```kotlin
fun transformDataManipulationEvaluationOrder(node: PartiqlAst.Statement.Dml): PartiqlAst.Statement {
    val from = node.from?.let { transformFromSource(it) }
    val where = node.where?.let { transformStatementDml_where(node) }
    val dmlOperation = transformDmlOp(node.operation)
    val metas = transformMetas(node.metas)

    return PartiqlAst.build {
        dml(dmlOperation, from, where, metas)
    }
}
```



## Copying Data from `PartiqlAst.Expr`

The `PartiqlAst.Expr` and all the subtypes don’t yet have a `.copy` function yet (will be added once [copy()](https://github.com/partiql/partiql-ir-generator/commit/66010109b09cc2ca6ee17a06517129c5d6a2ef84) is released by PIG). This adds slightly more work when trying to create an instance of `PartiqlAst.Expr` from another. A workaround is to create a helper function as follows:


```kotlin
/**
  * Copies all parts of [PartiqlAst.Expr.Select] except [newProjection] for [PartiqlAst.Projection].
  */
private fun copyProjectionToSelect(node: PartiqlAst.Expr.Select, newProjection: PartiqlAst.Projection): PartiqlAst.Expr {
    return PartiqlAst.build {
        select(
            setq = node.setq,
            project = newProjection,
            from = node.from,
            fromLet = node.fromLet,
            where = node.where,
            group = node.group,
            having = node.having,
            limit = node.limit,
            metas = node.metas)
    }
}
```


Another annoyance you may run into is changing/copying `metas` for the generic `PartiqlAst.Expr`. This will be trivial once [copyMetas()](https://github.com/partiql/partiql-ir-generator/commit/66010109b09cc2ca6ee17a06517129c5d6a2ef84) is released by PIG. A current workaround is mentioned here https://github.com/partiql/partiql-lang-kotlin/issues/311. You can create an inner class that overrides `transformMetas`.

```kotlin
inner class MetaVisitorTransform(private val newMetas: MetaContainer) : PartiqlAst.VisitorTransform() {
    override fun transformMetas(metas: MetaContainer): MetaContainer = newMetas
}
```

So in the `ExprNode` rewriters, rather than calling

```kotlin
node.copy(newMetaContainer)
```

For `PartiqlAst` transformers, you would call

```kotlin
MetaVisitorTransform(newMetaContainer).transformExpr(node)
```


## Additional Notes:

* Some checks that are part of `ExprNode` rewrites easier using PIG `VisitorTransforms`. These (non-exhaustively) include checks that select star projection isn’t included with other projection items and clause arity checks.
* General NAry operations in `ExprNode` are easier to rewrite (like in [PartiqlEvaluationRewriter.kt](https://github.com/partiql/partiql-lang-kotlin/blob/36fe55b6457f0dc82c02da6975358c01dd13aa3b/examples/src/kotlin/org/partiql/examples/PartialEvaluationRewriter.kt)) than transforming in the PIG domain. This is because currently in PIG, NAry operations are directly `PartiqlAst.Expr` types. Once these operations are better modeled as NAry operations (https://github.com/partiql/partiql-lang-kotlin/issues/241), it will become easier to reuse code.
* After making `FooVisitorTransform` (copied from `FooVisitorRewriter`), delete the old imports, so you can be sure previous `ExprNode` types aren’t used. This is helpful when `ExprNode` types and `PIG` domain have the same type names
* There may be still be bugs in the conversion process between `ExprNode` and `PartiqlAst` (in [ExprNodeToStatement.kt](https://github.com/partiql/partiql-lang-kotlin/blob/36fe55b6457f0dc82c02da6975358c01dd13aa3b/lang/src/org/partiql/lang/ast/ExprNodeToStatement.kt) and [StatementToExprNode.kt](https://github.com/partiql/partiql-lang-kotlin/blob/36fe55b6457f0dc82c02da6975358c01dd13aa3b/lang/src/org/partiql/lang/ast/StatementToExprNode.kt)) that cause errors, especially when copying over the metas.

## Sample Rewriter to VisitorTransform Conversions

* [SelectListItemAliasRewriter](https://github.com/partiql/partiql-lang-kotlin/pull/356/commits/548606c19b162e3a048aa4e2c6757aeaaf2bba87)
* [FromSourceAliasRewriter](https://github.com/partiql/partiql-lang-kotlin/pull/356/commits/b53778c04237e7c6590cf277a5732513a01319ff)
* [AggregateRewriterSupport](https://github.com/partiql/partiql-lang-kotlin/pull/356/commits/f0eb800d02d002581a714f04c3fc5a038adcb4d1)
* [GroupByItemAliasRewriter](https://github.com/partiql/partiql-lang-kotlin/pull/356/commits/29aa1d0d9a34e9c399258b79e4538d79bec6f2d0)
* [GroupByPathExpressionRewriter and SubstitutionRewriter](https://github.com/partiql/partiql-lang-kotlin/pull/356/commits/027960cdd33fb0a49a858837c69fac3c2a624dd8)
* [SelectStarRewriter](https://github.com/partiql/partiql-lang-kotlin/pull/356/commits/63a4885b530d754ba3258c563dc78a89ca48a2bb)
* [PartialEvaluation example](https://github.com/partiql/partiql-lang-kotlin/pull/356/commits/14875114d997c227f363d57008d2d9a4b17edf6d)
* [PipelinedRewriter](https://github.com/partiql/partiql-lang-kotlin/pull/356/commits/c2cdf246b4096618c0a4cd497abfe4effee0db04)
* [StaticTypeRewriter](https://github.com/partiql/partiql-lang-kotlin/pull/356/commits/c224d9684500f7ea9a211431d8395e7f3de1a33e)

