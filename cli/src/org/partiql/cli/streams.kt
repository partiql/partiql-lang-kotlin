/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.cli

import java.io.*

/**
 * Output stream that flushes instead of closing, useful for decorating output streams that should not be closed but you
 * still want to use them in a use block.
 */
internal class UnclosableOutputStream(out: OutputStream) : FilterOutputStream(out) {
    override fun close() {
        out.flush()
    }
}

/**
 * Input stream that does nothing instead of closing, useful for decorating input streams that should not be closed but you
 * still want to use them in a use block.
 */
internal class UnclosableInputStream(input: InputStream) : FilterInputStream(input) {
    override fun close() {
        // no-op
    }
}