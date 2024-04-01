package org.partiql.sprout.generator.target.kotlin.poems

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import org.partiql.sprout.generator.target.kotlin.KotlinPoem
import org.partiql.sprout.generator.target.kotlin.KotlinSymbols
import org.partiql.sprout.generator.target.kotlin.spec.KotlinPackageSpec
import org.partiql.sprout.generator.target.kotlin.spec.KotlinUniverseSpec
import org.partiql.sprout.generator.target.kotlin.types.Annotations.DO_NOT_IMPLEMENT_INTERFACE

/**
 * Poem which creates the [DO_NOT_IMPLEMENT_INTERFACE] annotation. This annotation require [OptIn] for the interface to
 * be implemented or extended.
 */
class KotlinDoNotImplementInterfacePoem(symbols: KotlinSymbols) : KotlinPoem(symbols) {

    override val id = DO_NOT_IMPLEMENT_INTERFACE

    private val annotationPackageName = "${symbols.rootPackage}.annotation"

    private val annotationFileName = "${DO_NOT_IMPLEMENT_INTERFACE}Annotation"

    override fun apply(universe: KotlinUniverseSpec) {
        universe.packages.add(
            KotlinPackageSpec(
                name = annotationPackageName,
                files = mutableListOf(universe.annotation()),
            )
        )
        super.apply(universe)
    }

    // --- Internal ----------------------------------------------------
    private fun KotlinUniverseSpec.annotation(): FileSpec {
        val annotation = TypeSpec.classBuilder(DO_NOT_IMPLEMENT_INTERFACE)
            .addModifiers(KModifier.PUBLIC, KModifier.ANNOTATION)
            .addAnnotation(
                AnnotationSpec.builder(ClassName(annotationPackageName, listOf("RequiresOptIn")))
                    .addMember("message = %S", "$DO_NOT_IMPLEMENT_INTERFACE requires explicit opt-in")
                    .addMember("level = RequiresOptIn.Level.ERROR")
                    .build()
            )
            .build()
        return FileSpec.builder(annotationPackageName, annotationFileName)
            .addType(annotation)
            .build()
    }
}
