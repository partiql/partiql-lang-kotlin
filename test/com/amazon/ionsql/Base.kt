/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import com.amazon.ion.IonValue
import com.amazon.ion.system.IonSystemBuilder
import org.junit.Assert

open class Base : Assert() {
    val ion = IonSystemBuilder.standard().build()

    fun literal(text: String): IonValue = ion.singleValue(text)
}