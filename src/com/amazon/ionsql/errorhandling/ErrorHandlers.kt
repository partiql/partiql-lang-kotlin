/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.errorhandling

import com.amazon.ionsql.IonSqlException
import com.amazon.ionsql.eval.ExprValue

class DefaultErrorHandler : IErrorHandler {

    override fun handle(errorCode: ErrorCode, context: PropertyBag): ExprValue {
        throw IonSqlException("IMPLEMENT ME!")
    }

}
