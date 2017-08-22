/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.errors

import com.amazon.ionsql.IonSqlException
import com.amazon.ionsql.eval.ExprValue

class DefaultErrorHandler : ErrorHandler {

    override fun handle(errorCode: ErrorCode, context: PropertyValueMap): ExprValue {
        throw IonSqlException("IMPLEMENT ME!")
    }

}
