/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.partiql.spi.function

/**
 * Classifies a provider contract violation found by [LoadedRoutineProvider.load].
 */
public enum class RoutineProviderValidationReason {
    /** A provider inventory callback or its returned definition metadata could not be read. */
    PROVIDER_ACCESS_FAILED,

    /** A provider source name contains an empty segment. */
    EMPTY_SOURCE_SEGMENT,

    /** One provider declares the same exact source name more than once for one routine kind. */
    DUPLICATE_SOURCE_NAME,

    /** A routine definition has no overloads. */
    EMPTY_OVERLOADS,

    /** An overload signature name does not exactly equal its provider source-name leaf. */
    SIGNATURE_NAME_MISMATCH,

    /** One routine definition has multiple overloads with the same ordered parameter types. */
    DUPLICATE_OVERLOAD_SIGNATURE,
}
