/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import com.amazon.ion.IonValue

/** A simple mapping of name to [IonValue]. */
interface Environment {
    /** Looks up a name within the environment. */
    operator fun get(name: String): IonValue?
}