/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
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

package org.partiql.cli.utils

import java.io.File
import java.io.InputStream

internal sealed class InputSource {

    internal abstract fun stream(): InputStream

    internal class FileSource(private val file: File) : InputSource() {
        override fun stream(): InputStream = file.inputStream()
    }

    internal class StringSource(private val string: String) : InputSource() {
        override fun stream(): InputStream = string.byteInputStream(Charsets.UTF_8)
    }
}
