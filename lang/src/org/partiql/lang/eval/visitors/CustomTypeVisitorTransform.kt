package org.partiql.lang.eval.visitors

import org.partiql.lang.domains.PartiqlAst

/**
 * Transforms all the hard-coded type-nodes in the AST to [PartiqlAst.Type.CustomType].
 * It is used only after deserializing the old persisted ASTs to make them compatible with the new version of the PIG AST.
 * It should be non-existent once the persisted ASTs are migrated to the new version of PIG AST.
 * The tests for this visitor transform are covered in `SqlParserCustomTypeCatalogTests.kt`
 * TODO: Remove this VisitorTransform once https://github.com/partiql/partiql-lang-kotlin/issues/510 is resolved.
 */
class CustomTypeVisitorTransform : VisitorTransformBase(){

    override fun transformTypeEsBoolean(node: PartiqlAst.Type.EsBoolean): PartiqlAst.Type =
        PartiqlAst.build { customType(name = "es_boolean", metas = transformTypeEsBoolean_metas(node)) }

    override fun transformTypeEsInteger(node: PartiqlAst.Type.EsInteger): PartiqlAst.Type =
        PartiqlAst.build { customType(name = "es_integer", metas = transformTypeEsInteger_metas(node)) }

    override fun transformTypeEsText(node: PartiqlAst.Type.EsText): PartiqlAst.Type =
        PartiqlAst.build { customType("es_text", metas = transformTypeEsText_metas(node)) }

    override fun transformTypeEsAny(node: PartiqlAst.Type.EsAny): PartiqlAst.Type =
        PartiqlAst.build { customType("es_any", metas = transformTypeEsAny_metas(node)) }

    override fun transformTypeEsFloat(node: PartiqlAst.Type.EsFloat): PartiqlAst.Type =
        PartiqlAst.build { customType("es_float", metas = transformTypeEsFloat_metas(node)) }

    override fun transformTypeRsBigint(node: PartiqlAst.Type.RsBigint): PartiqlAst.Type =
        PartiqlAst.build { customType("rs_bigint", metas = transformTypeRsBigint_metas(node)) }

    override fun transformTypeRsBoolean(node: PartiqlAst.Type.RsBoolean): PartiqlAst.Type =
        PartiqlAst.build { customType("rs_boolean", metas = transformTypeRsBoolean_metas(node)) }

    override fun transformTypeRsDoublePrecision(node: PartiqlAst.Type.RsDoublePrecision): PartiqlAst.Type =
        PartiqlAst.build { customType("rs_double_precision", metas = transformTypeRsDoublePrecision_metas(node)) }

    override fun transformTypeRsInteger(node: PartiqlAst.Type.RsInteger): PartiqlAst.Type =
        PartiqlAst.build { customType("rs_integer", metas = transformTypeRsInteger_metas(node)) }

    override fun transformTypeRsReal(node: PartiqlAst.Type.RsReal): PartiqlAst.Type =
        PartiqlAst.build { customType("rs_real", metas = transformTypeRsReal_metas(node)) }

    override fun transformTypeRsVarcharMax(node: PartiqlAst.Type.RsVarcharMax): PartiqlAst.Type =
        PartiqlAst.build { customType("rs_varchar_max", metas = transformTypeRsVarcharMax_metas(node)) }

    override fun transformTypeSparkBoolean(node: PartiqlAst.Type.SparkBoolean): PartiqlAst.Type =
        PartiqlAst.build { customType("spark_boolean", metas = transformTypeSparkBoolean_metas(node)) }

    override fun transformTypeSparkDouble(node: PartiqlAst.Type.SparkDouble): PartiqlAst.Type =
        PartiqlAst.build { customType("spark_double", metas = transformTypeSparkDouble_metas(node)) }

    override fun transformTypeSparkFloat(node: PartiqlAst.Type.SparkFloat): PartiqlAst.Type =
        PartiqlAst.build { customType("spark_float", metas = transformTypeSparkFloat_metas(node)) }

    override fun transformTypeSparkInteger(node: PartiqlAst.Type.SparkInteger): PartiqlAst.Type =
        PartiqlAst.build { customType("spark_integer", metas = transformTypeSparkInteger_metas(node)) }

    override fun transformTypeSparkLong(node: PartiqlAst.Type.SparkLong): PartiqlAst.Type =
        PartiqlAst.build { customType("spark_long", metas = transformTypeSparkLong_metas(node)) }

    override fun transformTypeSparkShort(node: PartiqlAst.Type.SparkShort): PartiqlAst.Type =
        PartiqlAst.build { customType("spark_short", metas = transformTypeSparkShort_metas(node)) }
}