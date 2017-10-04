package com.amazon.ionsql.util

import org.assertj.core.api.*

internal fun softAssert(assertions: SoftAssertions.() -> Unit) = SoftAssertions().apply(assertions).assertAll()