// /*
//  * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
//  *
//  *  Licensed under the Apache License, Version 2.0 (the "License").
//  *  You may not use this file except in compliance with the License.
//  *  A copy of the License is located at:
//  *
//  *       http://aws.amazon.com/apache2.0/
//  *
//  *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
//  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
//  *  language governing permissions and limitations under the License.
//  */
//
// package org.partiql.spi.internal.fn
//
// import org.partiql.spi.BindingCase
// import org.partiql.spi.BindingPath
// import org.partiql.types.function.FunctionSignature
//
// /**
//  * Helper class
//  */
// internal class FnRegistry {
//
//     /**
//      * Index of scalar functions defined within a catalog.
//      */
//     private val fns: org.partiql.spi.internal.fn.FnIndex<FunctionSignature.Scalar> =
//         _root_ide_package_.org.partiql.spi.internal.fn.FnIndexTree()
//
//     /**
//      * Index of scalar operators (special form scalar functions) defined within a catalog.
//      */
//     private val ops: org.partiql.spi.internal.fn.FnIndex<FunctionSignature.Scalar> =
//         _root_ide_package_.org.partiql.spi.internal.fn.FnIndexTree()
//
//     /**
//      * Index of aggregation function signatures defined within a catalog.
//      */
//     private val aggs: org.partiql.spi.internal.fn.FnIndex<FunctionSignature.Aggregation> =
//         _root_ide_package_.org.partiql.spi.internal.fn.FnIndexTree()
//
//     internal fun getScalar(path: BindingPath) = fns.search(path.normalized())
//
//     internal fun getOperator(path: BindingPath) = ops.search(path.normalized())
//
//     internal fun getAggregation(path: BindingPath) = aggs.search(path.normalized())
// }
