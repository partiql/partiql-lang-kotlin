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

package org.partiql.planner.test

/**
 * Holding class for test input.
 *
 * --#[example-test]
 * SELECT * FROM example;
 */
public data class PartiQLTest(
    public val key: Key,
    public val statement: String,
) {

    /**
     * Unique test identifier.
     *
     * @property group
     * @property name
     */
    public data class Key(
        public val group: String,
        public val name: String,
    )
}
