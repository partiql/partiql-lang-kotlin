/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.partiql.cli.format

// internal object ExplainFormatter {
//
//     internal fun format(result: PartiQLResult.Explain.Domain): String {
//         val format = result.format?.toUpperCase() ?: ExplainFormats.ION_SEXP.name
//         val formatOption = ExplainFormats.valueOf(format)
//         return formatOption.formatter.format(result.value)
//     }
//
//     private enum class ExplainFormats(val formatter: NodeFormatter) {
//         ION_SEXP(SexpFormatter),
//         TREE(TreeFormatter),
//         DOT(DotFormatter),
//         DOT_URL(DotUrlFormatter)
//     }
// }
