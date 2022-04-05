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

@file:Suppress("DEPRECATION") // We don't need warnings about ExprNode deprecation.

package org.partiql.lang

import com.amazon.ion.IonSystem
import junitparams.JUnitParamsRunner
import org.junit.Assert
import org.junit.runner.RunWith
import org.partiql.lang.eval.ExprValueFactory

@RunWith(JUnitParamsRunner::class)
abstract class TestBase : Assert() {

    val ion: IonSystem = ION
    val valueFactory = ExprValueFactory.standard(ion)

}
