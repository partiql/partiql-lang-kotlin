/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import com.amazon.ion.IonSequence
import com.amazon.ion.IonValue

class IonExprValue(override val ionValue: IonValue) : ExprValue {
    override fun bindOver(parent: Bindings): Bindings {
        throw UnsupportedOperationException("TODO")
    }

    override fun iterator(): Iterator<ExprValue> {
        throw UnsupportedOperationException("TODO")
    }
}