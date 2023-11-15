package org.partiql.lang

import com.amazon.ion.IonSystem
import junitparams.JUnitParamsRunner
import org.junit.Assert
import org.junit.runner.RunWith

/**
 * Most inheriting test classes access JUnit4's `assert*` methods through the protected methods of [TestBase]'s
 * super-class: [Assert].  This is less than ideal for a number of reasons, we should consider removing it as
 * part of one of the following issues:
 *
 * - https://github.com/partiql/partiql-lang-kotlin/issues/576
 * - https://github.com/partiql/partiql-lang-kotlin/issues/577
 */
@RunWith(JUnitParamsRunner::class)
abstract class TestBase : Assert() {

    val ion: IonSystem = ION
}
