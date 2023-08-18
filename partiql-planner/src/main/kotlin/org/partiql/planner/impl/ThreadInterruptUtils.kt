/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at:
 *
 *       http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.planner.impl

/** Throws [InterruptedException] if [Thread.interrupted] is set. */
internal fun checkThreadInterrupted() {
    if (Thread.interrupted()) {
        throw InterruptedException()
    }
}

/**
 * Like a regular [map], but checks [Thread.interrupted] before each iteration and throws
 * [InterruptedException] if it is set.
 *
 * This should be used instead of the regular [map] where there is a potential for a large
 * number of items in the receiver [List] to allow long running operations to be aborted
 * by the caller.
 */
internal inline fun <T, R> List<T>.interruptibleMap(crossinline block: (T) -> R): List<R> =
    this.map { checkThreadInterrupted(); block(it) }

/**
 * Like a regular [fold], but checks [Thread.interrupted] before each iteration and throws
 * [InterruptedException] if it is set.
 *
 * This should be used instead of the regular [fold] where there is a potential for a large
 * number of items in the receiver [List] to allow long running operations to be aborted
 * by the caller.
 */
internal inline fun <T, A> List<T>.interruptibleFold(initial: A, crossinline block: (A, T) -> A) =
    this.fold(initial) { acc, curr -> checkThreadInterrupted(); block(acc, curr) }
