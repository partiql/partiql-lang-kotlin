/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

/** General exception class for Ion SQL. */
open class IonSqlException(message: String, cause: Throwable? = null)
    : RuntimeException(message, cause)
