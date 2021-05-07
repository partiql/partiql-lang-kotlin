/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.ast

import com.amazon.ion.*
import org.partiql.lang.util.*

/**
 * Deserializes an s-expression based AST.
 *
 * The version of the s-expression based AST is determined by the implementation, possibly during deserialization.
 *
 * Implementations of this should not be assumed to be thread-safe.
 */
interface AstDeserializer {
    fun deserialize(sexp: IonSexp): ExprNode
}

/**
 * Deserializes an instance of a node meta from its s-expression representation.
 */
interface MetaDeserializer {
    /**
     * The tag of the meta which this [MetaDeserializer] can deserialize.
     */
    val tag: String

    /**
     * Perform deserialization of the given s-exp into an instance of [Meta].
     */
    fun deserialize(sexp: IonSexp): Meta
}

/**
 * Contains the metadata needed to perform a simple validation of s-expressions.
 */
private class SexpValidationRules(arityFrom: Int, arityTo: Int) {
    val arityRange = IntRange(arityFrom, arityTo)
}

/**
 * Specifies the "definition" of an s-expression tag.
 *
 * That is, the tag's text and it's arity range for each version of AST.
 *
 * Use the primary constructor when a node tag's arity varies from one version of the AST to the next.
 */
private class TagDefinition(val tagText: String, val validationRules: Map<AstVersion, SexpValidationRules>) {
    /**
     * Specifies an s-expression tag and a single version and arity range.
     *
     * Use this when a tag is only supported in a single version of the AST.
     */
    constructor(text: String, version: AstVersion, arityFrom: Int, arityTo: Int = arityFrom)
        : this(text, mapOf(version to SexpValidationRules(arityFrom, arityTo)))

    /**
     * Specifies an s-expression tag that is the same in all supported versions of the AST--that is:
     * in all versions identified by the [AstVersion] `enum class`.
     *
     * Use this when the tag is supported in all versions and the arity is the same in all versions.
     */
    constructor(text: String, arityFrom: Int, arityTo: Int = arityFrom)
        : this(text, AstVersion.values(), arityFrom, arityTo)

    /**
     * Specifies an s-expression tag that is only valid in a subset of the versions of the AST.
     *
     * Use this when the tag is not supported in all versions but the arity is the same in all
     * versions that are supported.
     */
    constructor(text: String, versions: Array<AstVersion>, arityFrom: Int, arityTo: Int = arityFrom)
        : this(text, versions.map { Pair(it, SexpValidationRules(arityFrom, arityTo)) }.toMap())
}

/**
 * Defines all possible tags that can exist as complete expressions that have a value.
 *
 * The [TagDefinition] provides additional context which will be extremely useful for maintaining
 * this over the long term and is used to perform tag validation and s-expression arity that is
 * correct for the specific version of AST in use.
 */
private enum class NodeTag(val definition: TagDefinition) {

    //Valid expressions:
    META(
        TagDefinition(
            "meta",
            mapOf(
                AstVersion.V0 to SexpValidationRules(2, 2),
                AstVersion.V1 to SexpValidationRules(1, Int.MAX_VALUE)
        ))),
    TERM(TagDefinition("term", AstVersion.V1, 1, 2)),
    EXP(TagDefinition("exp", AstVersion.V1, 1, 1)),
    LIT(TagDefinition("lit", 1, 1)),
    MISSING(TagDefinition("missing", 0, 0)),
    ID(TagDefinition("id", 2, 2)),
    SELECT(TagDefinition("select", 2, 5)),
    PIVOT(TagDefinition("pivot", 2, 5)),
    PATH(TagDefinition("path", 2, Int.MAX_VALUE)),
    CALL_AGG(TagDefinition("call_agg", 3, 3)),
    CALL_AGG_WILDCARD(TagDefinition("call_agg_wildcard", 1, 1)),
    STRUCT(TagDefinition("struct", 0, Int.MAX_VALUE)),
    LIST(TagDefinition("list", 0, Int.MAX_VALUE)),
    BAG(TagDefinition("bag", 0, Int.MAX_VALUE)),
    UNPIVOT(TagDefinition("unpivot", 1, 1)),
    SIMPLE_CASE(TagDefinition("simple_case", 0, Int.MAX_VALUE)),
    SEARCHED_CASE(TagDefinition("searched_case", 1, Int.MAX_VALUE)),
    WHEN(TagDefinition("when", 1, 2)),
    ELSE(TagDefinition("else", 1)),

    NARY_NOT(TagDefinition("not", 1, Int.MAX_VALUE)),
    NARY_ADD(TagDefinition("+", 1, Int.MAX_VALUE)),
    NARY_SUB(TagDefinition("-", 1, Int.MAX_VALUE)),
    NARY_MUL(
        TagDefinition(
            "*",
            mapOf(
                AstVersion.V0 to SexpValidationRules(0, Int.MAX_VALUE),
                AstVersion.V1 to SexpValidationRules(2, Int.MAX_VALUE)
        ))),
    NARY_DIV(TagDefinition("/", 2, Int.MAX_VALUE)),
    NARY_MOD(TagDefinition("%", 2, Int.MAX_VALUE)),
    NARY_GT(TagDefinition(">", 2, Int.MAX_VALUE)),
    NARY_GTE(TagDefinition(">=", 2, Int.MAX_VALUE)),
    NARY_LT(TagDefinition("<", 2, Int.MAX_VALUE)),
    NARY_LTE(TagDefinition("<=", 2, Int.MAX_VALUE)),
    NARY_EQ(TagDefinition("=", 2, Int.MAX_VALUE)),
    NARY_IN(TagDefinition("in", 2, Int.MAX_VALUE)),
    NARY_NOT_IN(TagDefinition("not_in", AstVersion.V0, 2, Int.MAX_VALUE)),
    NARY_NE(TagDefinition("<>", 2, Int.MAX_VALUE)),
    NARY_AND(TagDefinition("and", 2, Int.MAX_VALUE)),
    NARY_OR(TagDefinition("or", 2, Int.MAX_VALUE)),
    NARY_LIKE(TagDefinition("like", 2, Int.MAX_VALUE)),
    NARY_NOT_LIKE(TagDefinition("not_like", AstVersion.V0, 2, Int.MAX_VALUE)),
    NARY_BETWEEN(TagDefinition("between", 3, Int.MAX_VALUE)),
    NARY_NOT_BETWEEN(TagDefinition("not_between", AstVersion.V0, 3, Int.MAX_VALUE)),
    NARY_CALL(TagDefinition("call", 1, Int.MAX_VALUE)),
    NARY_STRING_CONCAT(TagDefinition("||", 1, Int.MAX_VALUE)),
    NARY_UNION(TagDefinition("union", 2, Int.MAX_VALUE)),
    NARY_UNION_ALL(TagDefinition("union_all", 2, Int.MAX_VALUE)),
    NARY_EXCEPT(TagDefinition("except", 2, Int.MAX_VALUE)),
    NARY_EXCEPT_ALL(TagDefinition("except_all", 2, Int.MAX_VALUE)),
    NARY_INTERSECT(TagDefinition("intersect", 2, Int.MAX_VALUE)),
    NARY_INTERSECT_ALL(TagDefinition("intersect_all", 2, Int.MAX_VALUE)),

    TYPED_IS(TagDefinition("is", 2)),
    TYPED_IS_NOT(TagDefinition("is_not", AstVersion.V0, 2)),
    TYPED_CAST(TagDefinition("cast", 2)),

    // These are not expressions by themselves but are still valid tags

    // Valid within select or path...
    STAR(TagDefinition("star", AstVersion.V1, 0, 1)),

    //Only valid within (select ...)
    PROJECT(TagDefinition("project", 1)),
    PROJECT_ALL(TagDefinition("project_all", AstVersion.V0, 0, 1)),
    PATH_PROJECT_ALL(TagDefinition("path_project_all", AstVersion.V1, 1)),
    PROJECT_DISTINCT(TagDefinition("project_distinct", 1)),
    VALUE(TagDefinition("value", 1)),

    //Only valid within (select ...) or (pivot ...)
    FROM(TagDefinition("from", 1)),
    WHERE(TagDefinition("where", 1)),
    //Only valid within select
    HAVING(TagDefinition("having", 1)),
    LIMIT(TagDefinition("limit", 1)),
    GROUP(TagDefinition("group", 1, 2)),
    BY(TagDefinition("by", 1, Int.MAX_VALUE)),
    NAME(TagDefinition("name", 1)),
    GROUP_PARTIAL(TagDefinition("group_partial", 2)),

    //Only valid within PIVOT
    MEMBER(TagDefinition("member", 2)),

    // Mixed context:  select list alias or from source alias
    AS(TagDefinition("as", 2)),
    // From source alias
    AT(TagDefinition("at", 2)),

    /** @ scope qualifier, unfortunately can't be named "at" due to [AT]. */
    SCOPE_QUALIFIER(TagDefinition("@", 1)),

    // Only valid within a from clause
    INNER_JOIN(TagDefinition("inner_join", 2, 3)),
    LEFT_JOIN(TagDefinition("left_join", 2, 3)),
    OUTER_JOIN(TagDefinition("outer_join", 2, 3)),
    RIGHT_JOIN(TagDefinition("right_join", 2, 3)),

    // Only valided in a typed expression i.e. the second argument of (is ...) or (cast ...))
    TYPE(TagDefinition("type", 1, 3)),

    //Only valid as path components...
    CASE_INSENSITIVE(TagDefinition("case_insensitive", AstVersion.V0, 1)),
    CASE_SENSITIVE(TagDefinition("case_sensitive", AstVersion.V0, 1)),
    PATH_ELEMENT(TagDefinition("path_element", AstVersion.V1, 1, 2));

    companion object {
        private val tagLookup = NodeTag.values().map { Pair(it.definition.tagText, it) }.toMap()

        /**
         * Returns the [NodeTag] value given [tagText] or null if it was not found.
         */
        fun fromTagText(tagText: String): NodeTag? = tagLookup[tagText]
    }
}

class AstDeserializerBuilder(val ion: IonSystem) {
    private val metaDeserializers = mutableMapOf(
        SourceLocationMeta.deserializer.tag to SourceLocationMeta.deserializer,
        LegacyLogicalNotMeta.deserializer.tag to LegacyLogicalNotMeta.deserializer,
        IsImplictJoinMeta.deserializer.tag to IsImplictJoinMeta.deserializer,
        IsCountStarMeta.deserializer.tag to IsCountStarMeta.deserializer)

    fun withMetaDeserializer(deserializer: MetaDeserializer): AstDeserializerBuilder {
        metaDeserializers[deserializer.tag] = deserializer
        return this
    }

    /**
     * Builds an instance of [AstDeserializer] using previously provided parameters.
     *
     * The instance that is returned should not be accessed concurrently by different threads.
     */
    fun build(): AstDeserializer =
        // Note: .toMap() makes an immutable map.
        AstDeserializerImpl(ion, metaDeserializers.toMap())
}

/**
 * A [Meta] implementation used to store metas encountered during deserialization that do not
 * have a registered deserializer. This allows such metas to be retained during subsequent serialization.
 */
private data class UnknownMeta(override val tag: String, val metaSexp: IonSexp) : Meta {
    override fun serialize(writer: IonWriter) {
        metaSexp.forEach {
            it.system.newReader(it).use { ionValue ->
                ionValue.next()
                writer.writeValue(ionValue)
            }
        }
    }
}

internal class AstDeserializerImpl(
    val ion: IonSystem,
    private val metaDeserializers: Map<String, MetaDeserializer>
) : AstDeserializer {

    /**
     * The version of the AST we are currently deserializing.
     * This is modified only if the version wrapper is present in the AST being serialized.
     *
     * The existence of this mutable state means that instances should not be accessed concurrently by different threads.
     */
    internal var astVersion = DEFAULT_AST_VERSION

    companion object {
        private val DEFAULT_AST_VERSION = AstVersion.V0

        /** A memoized empty meta instance used by default for all new nodes unless a meta node exists. */
        private val emptyMetas = metaContainerOf()

    }

    private val IonSexp.nodeTag: NodeTag
        get() = NodeTag.fromTagText(this.tagText) ?: err("Unknown tag: '${this.tagText}'")

    override fun deserialize(sexp: IonSexp) =
        when (sexp.tagText) {
        // There's an AST version wrapper, so let's extract the version and set astVersion.
            "ast" -> {
                val versionValue = sexp.singleArgWithTag("version").asIonSexp().args.first().longValue().toInt()
                val root = sexp.singleArgWithTag("root").asIonSexp()

                // In case we get a very old or newer AST version that we don't support.
                astVersion = AstVersion.values().singleOrNull { it.number == versionValue }
                    ?: err(
                    "This version of AstDeserializer does not know how to deserialize an AST of " +
                    "version ${astVersion}.  Only versions ${AstVersion.versionsAsString} are supported.")


                val rootSexp = root.args.first().asIonSexp()
                validate(rootSexp)
                deserializeExprNode(rootSexp)
            }
            else  -> {
                astVersion = DEFAULT_AST_VERSION
                validate(sexp)
                deserializeExprNode(sexp)
            }
        }

    internal fun validate(rootSexp: IonSexp) {
        checkThreadInterrupted()

        val nodeTag = rootSexp.nodeTag // Throws if nodeTag is invalid for the current AstVersion
        val nodeArgs = rootSexp.args

        val rules = nodeTag.definition.validationRules[astVersion]
                    ?: err("Tag '${nodeTag.definition.tagText}' is not valid for AST version ${astVersion.number}.")

        if (!rules.arityRange.contains(nodeArgs.size)) {
            err("Incorrect arity of ${nodeArgs.size} for node '${nodeTag.definition.tagText}'.  Expected ${rules.arityRange}")
        }

        //Do not attempt to validate metas in V1...
        if (astVersion == AstVersion.V1 && nodeTag == NodeTag.META) {
            return
        }
        if (nodeTag != NodeTag.LIT) {
            nodeArgs.filterIsInstance<IonSexp>().forEach {
                validate(it)
            }
        }
    }

    /**
     * In version 0 of the AST, each node may optionally be wrapped in a `(meta <exp> { line: <line>, column: <col> })`
     * node, where `exp` contains the actual expression.  In this case, this function constitutes a [SourceLocationMeta]
     * from the second argument of `meta` which is then passed to [deserializeNode] as the single element of a
     * [MetaContainer] for use when instantiating the node representing `exp`.
     *
     * In version 1 of the AST, each node may be optionally wrapped in a `term` node like so:
     *
     * ```
     * (term
     *   (exp <exp>)
     *   (meta
     *     (meta_tag_1 <meta_data>)
     *     (meta_tag_2 <meta_data>)))
     * ```
     *
     * Each meta is parsed and a and added to a [MetaContainer] which is then passed to [deserializeNode].
     *
     * The [deserializeNode] argument must continue deserializing the node and does not itself need to be aware
     * of where its metas are located relative to itself in the s-expression.
     */
    private fun <T> deserializeMetaOrTerm(targetSexp: IonSexp, deserializeNode: (IonSexp, MetaContainer) -> T): T =
        when (astVersion) {
            AstVersion.V0 ->
                when (targetSexp.tagText) {
                // Expression has metas -- extract source location information and pass that to [block].
                    "meta" -> {
                        //extract meta
                        val struct = targetSexp.args[1].asIonStruct()
                        val lineNum = struct.field("line").longValue()
                        val charOffset = struct.field("column").longValue()
                        val locationMeta = SourceLocationMeta(lineNum, charOffset)

                        val expSexp = targetSexp.args[0].asIonSexp()
                        deserializeNode(expSexp, metaContainerOf(locationMeta))
                    }
                //Expression not wrapped in `meta` and therefore has no metas, pass empty MetaContainer to [deserializeNode].
                    else   -> {
                        deserializeNode(targetSexp, emptyMetas)
                    }
                }
            AstVersion.V1 ->
                when (targetSexp.tagText) {
                    "term" -> {
                        val expSexp = targetSexp.singleArgWithTag("exp").asIonSexp().args.first().asIonSexp()

                        val maybeMetas = targetSexp.singleArgWithTagOrNull("meta")

                        val metaContainer = maybeMetas
                                                ?.asIonSexp()
                                                ?.args
                                                ?.toList()
                                                ?.let { deserializeMetaContainer(it) }
                                            ?: emptyMetas

                        deserializeNode(expSexp, metaContainer)
                    }
                // Expression not wrapped in `term` and therefore has no metas, pass empty MetaContainer to
                // [deserializeNode].
                    else   -> {
                        deserializeNode(targetSexp, emptyMetas)
                    }
                }
        }

    private fun deserializeMetaContainer(metasSexp: List<IonValue>): MetaContainer {
        val outerSexps = metasSexp.toListOfIonSexp()

        val metas = outerSexps.map {
            val deserializer = metaDeserializers[it.tagText]
            if (deserializer == null) {
                UnknownMeta(it.tagText, it.args[0].asIonSexp())
            }
            else {
                deserializer.deserialize(it.args[0].asIonSexp())
            }
        }
        return metaContainerOf(metas)
    }

    private fun List<IonValue>.deserializeAllExprNodes(): List<ExprNode> = map { deserializeExprNode(it.asIonSexp()) }

    /**
     * Given a serialized AST, return its [ExprNode] representation.
     */
    internal fun deserializeExprNode(metaOrTermOrExp: IonSexp): ExprNode {
        checkThreadInterrupted()
        return deserializeMetaOrTerm(metaOrTermOrExp) { target, metas ->
            val nodeTag = target.nodeTag
            val targetArgs = target.args //args is an extension property--call it once for efficiency
            //.toList() forces immutability
            when (nodeTag) {
                NodeTag.LIT                -> deserializeLit(targetArgs, metas)
                NodeTag.MISSING            -> deserializeMissing(metas)
                NodeTag.ID                 -> deserializeId(targetArgs, metas)
                NodeTag.SCOPE_QUALIFIER    -> deserializeScopeQualifier(targetArgs, metas)
                NodeTag.SELECT             -> deserializeSelect(target, metas)
                NodeTag.PIVOT              -> deserializeSelect(target, metas)
                NodeTag.PATH               -> deserializePath(target)
                NodeTag.CALL_AGG           -> deserializeCallAgg(targetArgs, metas)
                NodeTag.CALL_AGG_WILDCARD  -> deserializeCallAggWildcard(targetArgs, metas)
                NodeTag.STRUCT             -> deserializeStruct(targetArgs, metas)
                NodeTag.LIST               -> deserializeList(targetArgs, metas)
                NodeTag.BAG                -> deserializeBag(targetArgs, metas)
                NodeTag.SIMPLE_CASE        -> deserializeSimpleCase(target, metas)
                NodeTag.SEARCHED_CASE      -> deserializeSearchedCase(target, metas)
                NodeTag.NARY_NOT           -> deserializeNAryNot(targetArgs, metas)
                NodeTag.NARY_ADD           -> deserializeNAryAdd(targetArgs, metas)
                NodeTag.NARY_SUB           -> deserializeNArySub(targetArgs, metas)
                NodeTag.NARY_MUL           -> deserializeNAryMul(targetArgs, metas)
                NodeTag.NARY_DIV           -> deserializeNAryDiv(targetArgs, metas)
                NodeTag.NARY_MOD           -> deserializeNAryMod(targetArgs, metas)
                NodeTag.NARY_GT            -> deserializeNAryGt(targetArgs, metas)
                NodeTag.NARY_GTE           -> deserializeNAryGte(targetArgs, metas)
                NodeTag.NARY_LT            -> deserializeNAryLt(targetArgs, metas)
                NodeTag.NARY_LTE           -> deserializeNAryLte(targetArgs, metas)
                NodeTag.NARY_EQ            -> deserializeNAryEq(targetArgs, metas)
                NodeTag.NARY_NE            -> deserializeNAryNe(targetArgs, metas)
                NodeTag.NARY_IN            -> deserializeNAryIn(targetArgs, metas)
                NodeTag.NARY_NOT_IN        -> deserializeNAryNotIn(targetArgs, metas)
                NodeTag.NARY_AND           -> deserializeNAryAnd(targetArgs, metas)
                NodeTag.NARY_OR            -> deserializeNAryOr(targetArgs, metas)
                NodeTag.NARY_LIKE          -> deserializeNAryLike(targetArgs, metas)
                NodeTag.NARY_NOT_LIKE      -> deserializeNAryNotLlike(targetArgs, metas)
                NodeTag.NARY_BETWEEN       -> deserializeNAryBetween(targetArgs, metas)
                NodeTag.NARY_NOT_BETWEEN   -> deserializeNAryNotBetween(targetArgs, metas)
                NodeTag.NARY_STRING_CONCAT -> deserializeNAryStringConcat(targetArgs, metas)
                NodeTag.NARY_CALL          -> deserializeNAryCall(targetArgs, metas)
                NodeTag.NARY_UNION         -> deserializeNAryUnion(targetArgs, metas)
                NodeTag.NARY_UNION_ALL     -> deserializeNAryUnionAll(targetArgs, metas)
                NodeTag.NARY_EXCEPT        -> deserializeNAryExcept(targetArgs, metas)
                NodeTag.NARY_EXCEPT_ALL    -> deserializeNAryExceptAll(targetArgs, metas)
                NodeTag.NARY_INTERSECT     -> deserializeNAryIntersect(targetArgs, metas)
                NodeTag.NARY_INTERSECT_ALL -> deserializeNAryIntersectAll(targetArgs, metas)
                NodeTag.TYPED_IS           -> deserializeTypedIs(targetArgs, metas)
                NodeTag.TYPED_IS_NOT       -> deserializeTypedIsNot(targetArgs, metas)
                NodeTag.TYPED_CAST         -> deserializeTypedCast(targetArgs, metas)

                // These are handled elsewhere
                NodeTag.META,
                NodeTag.TERM,
                NodeTag.EXP,

                // These can't be directly deserialized to ExprNode instances.
                NodeTag.PROJECT,
                NodeTag.PROJECT_DISTINCT,
                NodeTag.PROJECT_ALL,
                NodeTag.PATH_PROJECT_ALL,
                NodeTag.FROM,
                NodeTag.WHERE,
                NodeTag.HAVING,
                NodeTag.GROUP,
                NodeTag.GROUP_PARTIAL,
                NodeTag.BY,
                NodeTag.NAME,
                NodeTag.LIMIT,
                NodeTag.MEMBER,
                NodeTag.AS,
                NodeTag.AT,
                NodeTag.PATH_ELEMENT,
                NodeTag.UNPIVOT,
                NodeTag.INNER_JOIN,
                NodeTag.LEFT_JOIN,
                NodeTag.OUTER_JOIN,
                NodeTag.RIGHT_JOIN,
                NodeTag.CASE_INSENSITIVE,
                NodeTag.CASE_SENSITIVE,
                NodeTag.STAR,
                NodeTag.VALUE,
                NodeTag.WHEN,
                NodeTag.ELSE,
                NodeTag.TYPE               -> err("Invalid context for tag '${nodeTag.definition.tagText}'")
            }
        }
    }

    private fun deserializeLit(targetArgs: List<IonValue>, metas: MetaContainer) = Literal(targetArgs.first(), metas)
    private fun deserializeMissing(metas: MetaContainer) = LiteralMissing(metas)

    private fun deserializeId(targetArgs: List<IonValue>, metas: MetaContainer): VariableReference =
        VariableReference(
            targetArgs[0].asIonSymbol().stringValue(),
            CaseSensitivity.fromSymbol(targetArgs[1].asIonSymbol().stringValue()),
            ScopeQualifier.UNQUALIFIED,
            metas)


    private fun deserializeScopeQualifier(targetArgs: List<IonValue>, metas: MetaContainer): VariableReference {
        val qualifiedSexp = targetArgs[0].asIonSexp()
        return VariableReference(
            qualifiedSexp.args[0].asIonSymbol().stringValue()!!,
            CaseSensitivity.fromSymbol(qualifiedSexp.args[1].asIonSymbol().stringValue()),
            ScopeQualifier.LEXICAL,
            metas)
    }

    private fun deserializeCallAgg(targetArgs: List<IonValue>, metas: MetaContainer): CallAgg =
        CallAgg(
            VariableReference(
                targetArgs[0].asIonSymbol().stringValue(),
                CaseSensitivity.INSENSITIVE,
                ScopeQualifier.UNQUALIFIED, emptyMetas),
            SetQuantifier.valueOf(targetArgs[1].asIonSymbol().toString().toUpperCase()),
            deserializeExprNode(targetArgs[2].asIonSexp()), metas)

    private fun deserializeCallAggWildcard(targetArgs: List<IonValue>, metas: MetaContainer): CallAgg {
        if (targetArgs[0].asIonSymbol().stringValue() != "count") {
            err("Only the 'count' function may be invoked with 'call_agg_wildcard'")
        }

        return createCountStar(ion, metas)
    }

    private fun deserializeStruct(targetArgs: List<IonValue>, metas: MetaContainer): Struct {
        if (targetArgs.size % 2 != 0) {
            err("Arity of 'struct' node must be divisible by 2.")
        }
        val pairCount = targetArgs.size / 2

        val pairs = ArrayList<StructField>()
        for (i in 0..(pairCount - 1)) {
            val keyIndex = i * 2
            pairs.add(
                StructField(
                    deserializeExprNode(targetArgs[keyIndex].asIonSexp()),
                    deserializeExprNode(targetArgs[keyIndex + 1].asIonSexp())))
        }
        return Struct(pairs.toList(), metas)
    }

    private fun deserializeList(
        targetArgs: List<IonValue>,
        metas: MetaContainer
    ) = ListExprNode(targetArgs.deserializeAllExprNodes(), metas)

    private fun deserializeBag(
        targetArgs: List<IonValue>,
        metas: MetaContainer
    ) = Bag(targetArgs.deserializeAllExprNodes(), metas)

    private fun deserializeNAryNot(
        targetArgs: List<IonValue>,
        metas: MetaContainer
    ) = NAry(NAryOp.NOT, targetArgs.deserializeAllExprNodes(), metas)

    private fun deserializeNAryAdd(
        targetArgs: List<IonValue>,
        metas: MetaContainer
    ) = NAry(NAryOp.ADD, targetArgs.deserializeAllExprNodes(), metas)

    private fun deserializeNArySub(
        targetArgs: List<IonValue>,
        metas: MetaContainer
    ) = NAry(NAryOp.SUB, targetArgs.deserializeAllExprNodes(), metas)

    private fun deserializeNAryMul(
        targetArgs: List<IonValue>,
        metas: MetaContainer
    ) = NAry(NAryOp.MUL, targetArgs.deserializeAllExprNodes(), metas)

    private fun deserializeNAryDiv(
        targetArgs: List<IonValue>,
        metas: MetaContainer
    ) = NAry(NAryOp.DIV, targetArgs.deserializeAllExprNodes(), metas)

    private fun deserializeNAryMod(
        targetArgs: List<IonValue>,
        metas: MetaContainer
    ) = NAry(NAryOp.MOD, targetArgs.deserializeAllExprNodes(), metas)

    private fun deserializeNAryGt(
        targetArgs: List<IonValue>,
        metas: MetaContainer
    ) = NAry(NAryOp.GT, targetArgs.deserializeAllExprNodes(), metas)

    private fun deserializeNAryGte(
        targetArgs: List<IonValue>,
        metas: MetaContainer
    ) = NAry(NAryOp.GTE, targetArgs.deserializeAllExprNodes(), metas)

    private fun deserializeNAryLt(
        targetArgs: List<IonValue>,
        metas: MetaContainer
    ) = NAry(NAryOp.LT, targetArgs.deserializeAllExprNodes(), metas)

    private fun deserializeNAryLte(
        targetArgs: List<IonValue>,
        metas: MetaContainer
    ) = NAry(NAryOp.LTE, targetArgs.deserializeAllExprNodes(), metas)

    private fun deserializeNAryEq(
        targetArgs: List<IonValue>,
        metas: MetaContainer
    ) = NAry(NAryOp.EQ, targetArgs.deserializeAllExprNodes(), metas)

    private fun deserializeNAryNe(
        targetArgs: List<IonValue>,
        metas: MetaContainer
    ) = NAry(NAryOp.NE, targetArgs.deserializeAllExprNodes(), metas)

    private fun deserializeNAryAnd(
        targetArgs: List<IonValue>,
        metas: MetaContainer
    ) = NAry(NAryOp.AND, targetArgs.deserializeAllExprNodes(), metas)

    private fun deserializeNAryOr(
        targetArgs: List<IonValue>,
        metas: MetaContainer
    ) = NAry(NAryOp.OR, targetArgs.deserializeAllExprNodes(), metas)

    private fun deserializeNAryNotIn(
        targetArgs: List<IonValue>,
        metas: MetaContainer
    ): NAry = NAry(
        NAryOp.NOT,
        listOf(NAry(NAryOp.IN, targetArgs.deserializeAllExprNodes(), metas)),
        metas + metaContainerOf(LegacyLogicalNotMeta.instance))

    private fun deserializeNAryIn(
        targetArgs: List<IonValue>,
        metas: MetaContainer
    ) = NAry(NAryOp.IN, targetArgs.deserializeAllExprNodes(), metas)

    private fun deserializeNAryNotLlike(
        targetArgs: List<IonValue>,
        metas: MetaContainer
    ): NAry = NAry(
        NAryOp.NOT,
        listOf(NAry(NAryOp.LIKE, targetArgs.deserializeAllExprNodes(), metas)),
        metas + metaContainerOf(LegacyLogicalNotMeta.instance))

    private fun deserializeNAryLike(
        targetArgs: List<IonValue>,
        metas: MetaContainer
    ) = NAry(NAryOp.LIKE, targetArgs.deserializeAllExprNodes(), metas)

    private fun deserializeNAryNotBetween(
        targetArgs: List<IonValue>,
        metas: MetaContainer
    ): NAry = NAry(
        NAryOp.NOT,
        listOf(NAry(NAryOp.BETWEEN, targetArgs.deserializeAllExprNodes(), metas)),
        metas + metaContainerOf(LegacyLogicalNotMeta.instance))

    private fun deserializeNAryBetween(
        targetArgs: List<IonValue>,
        metas: MetaContainer
    ) = NAry(NAryOp.BETWEEN, targetArgs.deserializeAllExprNodes(), metas)

    private fun deserializeNAryStringConcat(
        targetArgs: List<IonValue>,
        metas: MetaContainer
    ) = NAry(NAryOp.STRING_CONCAT, targetArgs.deserializeAllExprNodes(), metas)

    private fun deserializeNAryCall(targetArgs: List<IonValue>, metas: MetaContainer): NAry {
        val functionReference = VariableReference(
            targetArgs[0].asIonSymbol().stringValue(),
            CaseSensitivity.INSENSITIVE,
            ScopeQualifier.UNQUALIFIED,
            emptyMetas)

        val argExprNodes = targetArgs.drop(1).deserializeAllExprNodes()
        return NAry(NAryOp.CALL, listOf(functionReference) + argExprNodes, metas)
    }

    private fun deserializeNAryUnion(
        targetArgs: List<IonValue>,
        metas: MetaContainer
    ) = NAry(NAryOp.UNION, targetArgs.deserializeAllExprNodes(), metas)

    private fun deserializeNAryUnionAll(
        targetArgs: List<IonValue>,
        metas: MetaContainer
    ) = NAry(NAryOp.UNION_ALL, targetArgs.deserializeAllExprNodes(), metas)

    private fun deserializeNAryExcept(
        targetArgs: List<IonValue>,
        metas: MetaContainer
    ) = NAry(NAryOp.EXCEPT, targetArgs.deserializeAllExprNodes(), metas)

    private fun deserializeNAryExceptAll(
        targetArgs: List<IonValue>,
        metas: MetaContainer
    ) = NAry(NAryOp.EXCEPT_ALL, targetArgs.deserializeAllExprNodes(), metas)

    private fun deserializeNAryIntersect(
        targetArgs: List<IonValue>,
        metas: MetaContainer
    ) = NAry(NAryOp.INTERSECT, targetArgs.deserializeAllExprNodes(), metas)

    private fun deserializeNAryIntersectAll(
        targetArgs: List<IonValue>,
        metas: MetaContainer
    ) = NAry(NAryOp.INTERSECT_ALL, targetArgs.deserializeAllExprNodes(), metas)

    private fun deserializeTypedIs(
        targetArgs: List<IonValue>,
        metas: MetaContainer
    ) = Typed(
        TypedOp.IS,
        deserializeExprNode(targetArgs[0].asIonSexp()),
        deserializeDataType(targetArgs[1]),
        metas)


    private fun deserializeTypedIsNot(
        targetArgs: List<IonValue>,
        metas: MetaContainer
    ): NAry = NAry(
        NAryOp.NOT,
        listOf(
            Typed(
                TypedOp.IS,
                deserializeExprNode(targetArgs[0].asIonSexp()),
                deserializeDataType(targetArgs[1]),
                metas)),
        metas + metaContainerOf(LegacyLogicalNotMeta.instance))

    private fun deserializeTypedCast(
        targetArgs: List<IonValue>,
        metas: MetaContainer
    ) = Typed(
        TypedOp.CAST,
        deserializeExprNode(targetArgs[0].asIonSexp()),
        deserializeDataType(targetArgs[1]),
        metas)

    private fun deserializeSelect(target: IonSexp, metas: MetaContainer): ExprNode {
        val children = target.args.toListOfIonSexp().map { Pair(it.nodeTag, it) }.toMap()
        var setQuantifier = SetQuantifier.ALL

        val projection = when (target.nodeTag) {
            NodeTag.SELECT -> {
                val project = children[NodeTag.PROJECT]
                              ?: children[NodeTag.PROJECT_DISTINCT]
                              ?: err("select node missing project or project_distinct")

                if (project.nodeTag == NodeTag.PROJECT_DISTINCT) {
                    setQuantifier = SetQuantifier.DISTINCT
                }

                val projectChild = project[1].asIonSexp()

                when (projectChild.nodeTag) {
                    NodeTag.VALUE -> SelectProjectionValue(deserializeExprNode(projectChild.args.first().asIonSexp()))
                    NodeTag.LIST  -> deserializeSelectListItems(projectChild)
                    else          -> errInvalidContext(projectChild.nodeTag)
                }
            }
            NodeTag.PIVOT  -> {
                val member = children[NodeTag.MEMBER] ?: err("(pivot ...) missing member node")
                val memberArgs = member.args
                val nameExpr = deserializeExprNode(memberArgs[0].asIonSexp())
                val valueExpr = deserializeExprNode(memberArgs[1].asIonSexp())

                SelectProjectionPivot(nameExpr, valueExpr)
            }
            else           -> errInvalidContext(target.nodeTag)
        }

        val from = children[NodeTag.FROM] ?: err("select node missing from")

        val fromExprNode = deserializeFromSource(from.args.first().asIonSexp())

        val whereExprNode = children[NodeTag.WHERE]?.let { deserializeExprNode(it.args.first().asIonSexp()) }
        val groupBy = (children[NodeTag.GROUP] ?: children[NodeTag.GROUP_PARTIAL])?.let {
            val bySexp = it.singleArgWithTag("by").asIonSexp()

            val items = bySexp.args.toListOfIonSexp().map { gbiSexp -> deserializeGroupByItem(gbiSexp.asIonSexp()) }

            val nameSymbol: SymbolicName? = it.singleWrappedChildWithTagOrNull("name")?.let { nameArg ->
                deserializeMetaOrTerm(nameArg.asIonSexp()) { target, metas ->
                    SymbolicName(
                        target.args[0].asIonSymbol().stringValue(),
                        metas)
                }
            }

            val groupingSrategy = when (it.nodeTag) {
                NodeTag.GROUP -> GroupingStrategy.FULL
                else          -> GroupingStrategy.PARTIAL
            }

            GroupBy(groupingSrategy, items, nameSymbol)
        }

        val havingExprNode = children[NodeTag.HAVING]?.let { deserializeExprNode(it.args.first().asIonSexp()) }
        val limitExprNode = children[NodeTag.LIMIT]?.let { deserializeExprNode(it.args.first().asIonSexp()) }


        return Select(
            setQuantifier = setQuantifier,
            projection = projection,
            from = fromExprNode,
            where = whereExprNode,
            groupBy = groupBy,
            having = havingExprNode,
            limit = limitExprNode,
            metas = metas)
    }

    private fun deserializeSelectListItems(projectChild: IonSexp): SelectProjectionList {
        val selectListItems = projectChild.args.map { selectListItemSexp ->
            deserializeMetaOrTerm(selectListItemSexp.asIonSexp()) { itemTarget, metas ->
                when (itemTarget.nodeTag) {
                    NodeTag.AS               -> {
                        val asName = SymbolicName(
                            itemTarget.args[0].asIonSymbol().stringValue(),
                            metas)

                        SelectListItemExpr(deserializeExprNode(itemTarget.args[1].asIonSexp()), asName)
                    }
                    NodeTag.PROJECT_ALL      -> {
                        if (itemTarget.arity == 0) {
                            SelectListItemStar(metas)
                        }
                        else {
                            SelectListItemProjectAll(
                                // Note:  metas is always empty in this case because the metas are on the child of
                                // PROJECT_ALL.  This means we should not call .copy() as in other cases.)
                                deserializeExprNode(itemTarget.args[0].asIonSexp()))
                        }
                    }
                    NodeTag.PATH_PROJECT_ALL -> {
                        SelectListItemProjectAll(
                            // Note:  metas is always empty in this case because the metas are on the child of
                            // PROJECT_ALL.  This means we should not call .copy() as in other cases.)
                            deserializeExprNode(itemTarget.args[0].asIonSexp()))
                    }
                    NodeTag.STAR             -> SelectListItemStar(metas)
                    else                     -> SelectListItemExpr(deserializeExprNode(selectListItemSexp.asIonSexp()))
                }
            }
        }

        return SelectProjectionList(selectListItems)
    }

    /**
     * On first invocation the [asName] and [atName] parameters should be null.
     *
     * If `(at anAlias ...)` is encountered in [fromSexp], we dispatch to the `deserializeFromSourceAt` method, which
     * extracts the alias and mutually recurses back into `deserializeFromSource` providing the alias constructed
     * from the argument of `(at anAlias ...)` as the [atAlias] argument.
     *
     * The same happens for `(as anAlias...)` and the [asAlias] argument.
     *
     * An exception is thrown:
     *
     * - The as/at pair are nested thusly:  `(as foo (at bar ...))` (the nesting must be `(at foo (as bar ...))`)
     * - If either `as` or `at` is encountered twice.
     *
     * If [fromSexp]'s tag is not `as` or `at` then an `unpivot` or join expression is deserialized as appropriate.
     */
    private fun deserializeFromSource(
        fromSexp: IonSexp,
        asName: SymbolicName? = null,
        atName: SymbolicName? = null
    ): FromSource = deserializeMetaOrTerm(fromSexp) { target, metas ->
        val targetArgs = target.args
        when (target.nodeTag) {
            NodeTag.AT         -> deserializeFromSourceAt(asName, atName, targetArgs, metas)
            NodeTag.AS         -> deserializeFromSourceAs(asName, targetArgs, atName, metas)
            NodeTag.UNPIVOT    -> deserializeFromSourceUnpivot(targetArgs, asName, atName, metas)
            NodeTag.INNER_JOIN,
            NodeTag.LEFT_JOIN,
            NodeTag.RIGHT_JOIN,
            NodeTag.OUTER_JOIN -> deserializeFromSourceJoin(atName, asName, target, targetArgs, metas)
            else               -> deserializeFromSourcExpr(target, metas, asName, atName)
        }
    }

    private fun deserializeFromSourceAt(
        asName: SymbolicName?,
        atName: SymbolicName?,
        targetArgs: List<IonValue>,
        metas: MetaContainer
    ): FromSource {
        if (asName != null) {
            err("When used together, 'as' must wrap 'at', i.e.:  (at ... (as ...))")
        }
        if (atName != null) {
            err("this from source was previously wrapped in an (at ...) s-exp.")
        }
        val actualAtName = SymbolicName(targetArgs[0].asIonSymbol().stringValue(), metas)
        return deserializeFromSource(targetArgs[1].asIonSexp(), asName, actualAtName)
    }

    private fun deserializeFromSourceAs(
        asName: SymbolicName?,
        targetArgs: List<IonValue>,
        atName: SymbolicName?,
        metas: MetaContainer
    ): FromSource {
        if (asName != null) {
            err("this from source was previously wrapped in an (as ...) s-exp.")
        }
        val actualAsName = SymbolicName(targetArgs[0].asIonSymbol().stringValue(), metas)
        return deserializeFromSource(targetArgs[1].asIonSexp(), actualAsName, atName)
    }

    private fun deserializeFromSourceUnpivot(
        targetArgs: List<IonValue>,
        asName: SymbolicName?,
        atName: SymbolicName?,
        metas: MetaContainer
    ): FromSourceUnpivot {
        val expr = deserializeExprNode(targetArgs[0].asIonSexp())
        return FromSourceUnpivot(expr, asName, atName, metas)
    }

    private fun deserializeFromSourceJoin(
        atName: SymbolicName?,
        asName: SymbolicName?,
        target: IonSexp,
        targetArgs: List<IonValue>,
        metas: MetaContainer
    ): FromSourceJoin {

        if (atName != null || asName != null) {
            err("${target.nodeTag.definition.tagText} is not allowed within (as ...) or (at ...)")
        }

        val joinOp = when (target.nodeTag) {
            NodeTag.INNER_JOIN -> JoinOp.INNER
            NodeTag.LEFT_JOIN  -> JoinOp.LEFT
            NodeTag.RIGHT_JOIN -> JoinOp.RIGHT
            NodeTag.OUTER_JOIN -> JoinOp.OUTER
            else               -> throw IllegalStateException(
                "Illegal join operator: ${target.nodeTag.definition.tagText}")
        }

        val leftFromSource = deserializeFromSource(targetArgs[0].asIonSexp())
        val rightFromSource = deserializeFromSource(targetArgs[1].asIonSexp())

        val (condition, metasMaybeWithImplicitJoin) = when {
            target.arity > 2 -> Pair(deserializeExprNode(targetArgs[2].asIonSexp()), metas)
            else             -> Pair(
                Literal(ion.newBool(true), emptyMetas),
                metas + metaContainerOf(IsImplictJoinMeta.instance))
        }

        return FromSourceJoin(
            joinOp,
            leftFromSource,
            rightFromSource,
            condition,
            metasMaybeWithImplicitJoin)
    }

    private fun deserializeFromSourcExpr(
        target: IonSexp,
        metas: MetaContainer,
        asName: SymbolicName?,
        atName: SymbolicName?
    ): FromSourceExpr {
        return FromSourceExpr(
            expr = deserializeExprNode(target).copy(metas),
            asName = asName,
            atName = atName)
    }

    private fun deserializeGroupByItem(target: IonSexp): GroupByItem =
        deserializeMetaOrTerm(target) { innerTarget, metas ->
            val innerTargetArgs = innerTarget.args
            when (innerTarget.nodeTag) {
                NodeTag.AS -> {
                    val symbolicName = SymbolicName(
                        innerTargetArgs[0].asIonSymbol().stringValue(),
                        metas)

                    val expr = deserializeExprNode(innerTargetArgs[1].asIonSexp())
                    GroupByItem(expr, symbolicName)
                }
                else       -> {
                    val expr = deserializeExprNode(innerTarget).copy(metas)
                    GroupByItem(expr, null)
                }
            }
        }

    private fun deserializeSimpleCase(target: IonSexp, metas: MetaContainer): ExprNode {
        val targetArgs = target.args
        val valueExpr = deserializeExprNode(targetArgs.first().asIonSexp())
        val clauses = targetArgs.drop(1).toListOfIonSexp()

        val whenClauses = clauses
            .filter { it.nodeTag == NodeTag.WHEN }
            .map {
                val whenValueExpr = deserializeExprNode(it.args[0].asIonSexp())
                val thenExpr = deserializeExprNode(it.args[1].asIonSexp())
                SimpleCaseWhen(whenValueExpr, thenExpr)
            }

        val elseClause = clauses.singleOrNull { it.nodeTag == NodeTag.ELSE }?.let {
            deserializeExprNode(it.args.first().asIonSexp())
        }

        return SimpleCase(valueExpr, whenClauses, elseClause, metas)
    }

    private fun deserializeSearchedCase(target: IonSexp, metas: MetaContainer): ExprNode {
        val targetArgs = target.args
        val clauses = targetArgs.toListOfIonSexp()

        val whenClauses = clauses
            .filter { it.nodeTag == NodeTag.WHEN }
            .map {
                val whenConditionExpr = deserializeExprNode(it.args[0].asIonSexp())
                val thenExpr = deserializeExprNode(it.args[1].asIonSexp())
                SearchedCaseWhen(whenConditionExpr, thenExpr)
            }

        val elseClause = clauses.singleOrNull { it.nodeTag == NodeTag.ELSE }?.let {
            deserializeExprNode(it.args.first().asIonSexp())
        }

        return SearchedCase(whenClauses, elseClause, metas)
    }

    private fun deserializePath(pathSexp: IonSexp) =
        deserializeMetaOrTerm(pathSexp) { target, metas ->
            val root = deserializeExprNode(target.args[0].asIonSexp())
            val componentSexps = pathSexp.args.drop(1).toListOfIonSexp()
            val pathComponents = deserializePathComponents(componentSexps)
            Path(root, pathComponents, metas)
        }


    private fun deserializePathComponents(componentSexps: List<IonSexp>): List<PathComponent> =
        when (astVersion) {
            AstVersion.V0 -> componentSexps.map { componentSexp ->
                val (targetComponent, caseSensitivity) = when (componentSexp.nodeTag) {
                    NodeTag.CASE_INSENSITIVE -> Pair(componentSexp.args[0].asIonSexp(), CaseSensitivity.INSENSITIVE)
                    NodeTag.CASE_SENSITIVE   -> Pair(componentSexp.args[0].asIonSexp(), CaseSensitivity.SENSITIVE)
                    else                     -> Pair(componentSexp, CaseSensitivity.SENSITIVE)
                }
                deserializeMetaOrTerm(targetComponent) { target, metas ->
                    // Note:  not sure why, but without this assignment (to variable 'pc')
                    // the Kotlin compiler believes this `when` expression has a type of Any...
                    // It does not appear to be smart enough to realize that PathComponentWildCard
                    // and PathComponentUnpivot have the same base class.
                    val pc: PathComponent = when (target.nodeTag) {
                        NodeTag.STAR, NodeTag.NARY_MUL -> {
                            when (target.arity) {
                                0    -> PathComponentWildcard(metas)
                                1    -> {
                                    if (target.args[0].asIonSymbol().stringValue() != "unpivot") {
                                        err("Invalid argument to '(star)' or '(*)' in path component. Expected no argument or 'unpivot'")
                                    }
                                    PathComponentUnpivot(metas)
                                }
                                else -> throw IllegalStateException(
                                    "invalid arity for (star) or (*) (this should have been caught earlier)")
                            }
                        }
                        else                           -> {
                            val exprNode = deserializeExprNode(target).copy(metas)
                            PathComponentExpr(exprNode, caseSensitivity)
                        }
                    }
                    pc
                }
            }
            AstVersion.V1 -> {
                componentSexps.map { pathCompnent ->
                    when (pathCompnent.nodeTag) {
                        NodeTag.PATH_ELEMENT -> {
                            deserializeMetaOrTerm(pathCompnent.args[0].asIonSexp()) { targetComponent, metas ->
                                when (targetComponent.nodeTag) {
                                    NodeTag.STAR -> {
                                        // Note:  not sure why, but without this assignment (to variable 'pc')
                                        // the Kotlin compiler believes this `when` expression has a type of Any...
                                        // It does not appear to be smart enough to realize that PathComponentWildCard
                                        // and PathComponentUnpivot have the same base class.
                                        val pc: PathComponent = when (targetComponent.arity) {
                                            0    -> PathComponentWildcard(metas)
                                            1    -> {
                                                if (targetComponent.args[0].asIonSymbol().stringValue() != "unpivot") {
                                                    err("Invalid argument to '*' in path component. Expected no argument or 'unpivot'")
                                                }
                                                PathComponentUnpivot(metas)

                                            }
                                            else -> throw IllegalStateException(
                                                "invalid arity for ${NodeTag.STAR.definition.tagText} (this should have been caught earlier)")
                                        }
                                        pc
                                    }
                                    else                           -> {
                                        //This ExprNode's metas were already consumed above... need to add them back to the ExprNode
                                        val exprNode = deserializeExprNode(targetComponent).copy(metas)

                                        val caseSensitivity = when (pathCompnent.arity) {
                                            1    -> CaseSensitivity.INSENSITIVE //When not specified, this is the default
                                            2    -> {
                                                val marker = pathCompnent.args[1].asIonSymbol().stringValue()
                                                when (marker) {
                                                    "case_sensitive"   -> CaseSensitivity.SENSITIVE
                                                    "case_insensitive" -> CaseSensitivity.INSENSITIVE
                                                    else               -> err("Expected 'case_sensitive' or 'case_insensitive', found ${marker}")
                                                }
                                            }
                                            else -> throw IllegalStateException(
                                                "invalid arity for ${NodeTag.PATH_ELEMENT.definition.tagText} (this should have been caught earlier)")
                                        }
                                        PathComponentExpr(exprNode, caseSensitivity)
                                    }
                                }
                            }
                        }
                        else                 -> errInvalidContext(pathCompnent.nodeTag)
                    }
                }
            }
        }

    private fun deserializeDataType(dataTypeSexp: IonValue): DataType =
        deserializeMetaOrTerm(dataTypeSexp.asIonSexp()) { target, metas ->
            val nodeTag = target.nodeTag
            when (nodeTag) {
                NodeTag.TYPE -> {
                    val typeName = target.args[0].asIonSymbol().stringValue()
                    val sqlDataType = SqlDataType.forTypeName(typeName) ?: err("'$typeName' is not a valid data type")
                    val args = target.args.drop(1).map { it.asIonInt().longValue() }
                    if (!sqlDataType.arityRange.contains(args.size)) {
                        err("Type $typeName arity range ${sqlDataType.arityRange} was but ${args.size} were specified")
                    }
                    DataType(sqlDataType, args, metas)
                }
                else         -> {
                    err("Expected `${NodeTag.TYPE.definition.tagText}` tag instead found ${nodeTag.definition.tagText}")
                }
            }
        }

    /**
     * Locates a child node of the receiving [IonSexp] which has a tag name matching [tagName].
     *
     * If a child node is wrapped in a `meta` or `term` (depending on the value of [astVersion]),
     * it is unwrapped before performing the comparison.
     */
    private fun IonSexp.singleWrappedChildWithTagOrNull(tagName: String): IonValue? =
        when(astVersion) {
            AstVersion.V0 ->
                this.args.map { it.asIonSexp() }.singleOrNull {
                    val tagText = when(it.tagText) {
                        "meta" -> it.args[0].asIonSexp().tagText
                        else -> it.tagText
                    }
                    tagText == tagName
                }

            AstVersion.V1 ->
                this.args.map { it.asIonSexp() }.singleOrNull {
                    val tagText = when(it.tagText) {
                        "term" -> it.singleArgWithTag("exp").asIonSexp().args[0].asIonSexp().tagText
                        else -> it.tagText
                    }
                    tagText == tagName
                }

        }

}

private fun err(message: String): Nothing =
    throw IllegalArgumentException(message)

private fun errInvalidContext(nodeTag: NodeTag): Nothing =
    throw IllegalArgumentException("Invalid context for ${nodeTag.definition.tagText} node")
