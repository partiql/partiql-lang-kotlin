package org.partiql.ionschema.parser

import com.amazon.ionelement.api.ContainerElement
import com.amazon.ionelement.api.IonElement

internal fun <T : IonElement> T.requireSingleAnnotation(anno: String): T {
    when (this.annotations.size) {
        1 -> {
            if (this.annotations[0] != anno) {
                parseError(this, Error.UnexpectedAnnotation(this.annotations[0]))
            }
        }
        else -> parseError(this, Error.UnexpectedAnnotationCount(1..1, this.annotations.size))
    }
    return this
}

internal fun <T : IonElement> T.requireZeroAnnotations(): T {
    if (this.annotations.any()) {
        parseError(this, Error.UnexpectedAnnotationCount(0..0, this.annotations.size))
    }
    return this
}

internal fun <T : IonElement> T.allowSingleAnnotation(anno: String): Boolean =
    when (this.annotations.size) {
        0 -> false
        1 -> {
            val foundAnno = this.annotations[0]
            if (foundAnno != anno) {
                parseError(this, Error.AnnotationNotAllowedHere(foundAnno))
            }
            true
        }
        else -> parseError(this, Error.UnexpectedAnnotationCount(0..1, this.annotations.size))
    }

internal fun <T : IonElement> T.allowSingleAnnotation(validAnnos: Set<String>): Boolean =
    when (this.annotations.size) {
        0 -> false
        1 -> {
            val foundAnno = this.annotations[0]
            if (foundAnno !in validAnnos) {
                parseError(this, Error.AnnotationNotAllowedHere(foundAnno))
            }
            true
        }
        else -> parseError(this, Error.UnexpectedAnnotationCount(0..1, this.annotations.size))
    }

internal fun <T : IonElement> T.allowAnnotations(validAnnos: Set<String>): T =
    when (this.annotations.size) {
        in 0..validAnnos.size -> {
            this.annotations.forEach {
                if (it !in validAnnos) {
                    parseError(this, Error.AnnotationNotAllowedHere(it))
                }
            }
            this
        }
        else -> parseError(this, Error.UnexpectedAnnotationCount(0..1, this.annotations.size))
    }

internal fun <T : IonElement> T.requireUniqueAnnotations(): T {
    this.annotations
        .groupBy { it }.entries
        .firstOrNull { it.value.size > 1 }
        ?.let { (key, value) ->
            parseError(this, Error.DuplicateAnnotationsNotAllowed(key))
        }
    return this
}
internal fun <T : ContainerElement> T.requireSize(s: Int): T {
    if (this.size != s) {
        parseError(this, Error.UnexpectedListSize(s..s, this.size))
    }
    return this
}

internal fun <T : ContainerElement> T.requireNonzeroListSize(): T {
    if (this.size == 0) {
        parseError(this, Error.EmptyListNotAllowedHere)
    }
    return this
}
