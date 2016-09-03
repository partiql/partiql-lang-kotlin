/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import com.amazon.ion.IonValue

class IonExpressionValue(private val value: IonValue) : ExpressionValue {
    override fun ionize(): IonValue = value.clone()

    override fun bindOver(parent: Bindings): Bindings {
        throw UnsupportedOperationException("TODO")
    }

    override fun iterator(): Iterator<ExpressionValue> {
        throw UnsupportedOperationException("TODO")
    }

}