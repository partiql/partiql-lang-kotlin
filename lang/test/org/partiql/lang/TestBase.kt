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

package org.partiql.lang

import com.amazon.ion.IonSystem
import junitparams.JUnitParamsRunner
import org.junit.Assert
import org.junit.runner.RunWith
import org.partiql.lang.eval.ExprValueFactory

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
    val valueFactory = ExprValueFactory.standard(ion)
}
