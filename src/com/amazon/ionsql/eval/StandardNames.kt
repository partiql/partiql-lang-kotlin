/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

/** Constructs the column name based on the zero-based index of that column. */
fun syntheticColumnName(col: Int): String = "_${col + 1}"
