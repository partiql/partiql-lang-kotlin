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

/**
 * Represents a re-openable source containing input data. By calling [stream], the [InputSource] opens a stream at the
 * beginning of the represented data.
 */
internal sealed class InputSource {

    internal abstract fun stream(): InputStream

    /**
     * Represents a re-openable source wrapping a file's contents. By calling [stream], the [FileSource] opens a stream
     * at the beginning of the [file]
     */
    internal class FileSource(private val file: File) : InputSource() {
        override fun stream(): InputStream = file.inputStream()
    }

    /**
     * Represents a re-openable source wrapping a string's contents. By calling [stream], the [StringSource] opens a stream
     * at the beginning of the [string]
     */
    internal class StringSource(private val string: String) : InputSource() {
        override fun stream(): InputStream = string.byteInputStream(Charsets.UTF_8)
    }
}
