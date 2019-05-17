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

package org.partiql.testframework

import java.io.*

/**
 * List all files recursing over directories
 *
 * @param filter file filter, defaults to an empty filter
 *
 * @throws TestSuiteException if the file is not found
 */
internal fun File.listRecursive(filter: FileFilter = FileFilter { _ -> true }): List<File> = when {
    !this.exists()   -> throw FatalException("'${this.path}' not found")
    this.isDirectory -> this.listFiles(filter).flatMap { it.listRecursive(filter) }
    this.isFile      -> listOf(this)
    else             -> throw TestSuiteInternalException("couldn't read '${this.path}'. It's neither a file nor a directory")
}
