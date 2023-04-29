/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at:
 *
 *       http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.spi.connector

/**
 * An object's representation within a Catalog. This is used by plugin implementers to store logic in relation to the
 * [ConnectorMetadata]. An example implementation of [ConnectorObject] could represent an object of a Catalog that holds
 * the serialized [org.partiql.types.StaticType], so that the [ConnectorMetadata] may be able
 * to grab the descriptor using [ConnectorMetadata.getObjectType].
 */
interface ConnectorObject
