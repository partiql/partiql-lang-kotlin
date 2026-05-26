/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
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

package org.partiql.cli.format

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.partiql.parser.PartiQLParser

class PrettyPrinterTest {

    private val parser = PartiQLParser.builder().build()

    private fun fmt(sql: String, width: Int = 80): String {
        val result = parser.parse(sql)
        return result.statements.joinToString(";\n\n") { it.pretty(width = width) }
    }

    @Test
    fun complexQueryIsIdempotent() {
        val input = """
            SELECT pt.* FROM "CSMLA_RS"."O_CUSTOMER_TRAFFIC_HITS".V4 pt LEFT JOIN "AMAZON_CONSENT_ENGINE_PROD"."PRIVACY_SCOPES".V4 dma_privacy_scope ON cast(pt."marketplace_id" as bigint) = (CASE WHEN dma_privacy_scope.locality_type = 'marketplace_id' THEN cast(dma_privacy_scope.locality_value as bigint) ELSE NULL END) AND dma_privacy_scope.locality_type = 'marketplace_id' AND dma_privacy_scope.privacy_scope = 'EU_CROSS_LOB_DATA_SHARING_V1' AND dma_privacy_scope.is_deleted = 'N' LEFT JOIN "AMAZON_CONSENT_ENGINE_PROD"."CUSTOMER_CONSENT_DIRECTIVES".V4 dma_customer_consent ON dma_customer_consent.record_type = 'actual' AND dma_customer_consent.is_deleted = 'N' AND dma_customer_consent.id_type = 'customer' AND dma_customer_consent.customer_id = cast(pt."customer_id" as bigint) AND dma_customer_consent.requester_lob_id = (SELECT CASE WHEN count(lob_id)=1 THEN max(lob_id) ELSE 0 END as lob_id FROM BDT_ANALYTICS_PROD.D_ANDES_IDENTITY_TO_LOB_MAPPING.V2 dma_idty_to_lob_mapping WHERE identity_arn = current_user AND lob_id >=0 AND is_deleted = 'N') LEFT JOIN "AMAZON_CONSENT_ENGINE_PROD"."CUSTOMER_CONSENT_DIRECTIVES".V4 dma_fallback_consent ON dma_fallback_consent.record_type = 'default' AND dma_fallback_consent.is_deleted = 'N' AND dma_fallback_consent.id_type = 'customer' AND dma_fallback_consent.customer_id = 0 AND dma_fallback_consent.requester_lob_id = (SELECT CASE WHEN count(lob_id)=1 THEN max(lob_id) ELSE 0 END as lob_id FROM BDT_ANALYTICS_PROD.D_ANDES_IDENTITY_TO_LOB_MAPPING.V2 dma_idty_to_lob_mapping WHERE identity_arn = current_user AND lob_id >=0 AND is_deleted = 'N') WHERE NOT(NOT ( ( cast ( pt."marketplace_id" as BIGINT ) IS NOT NULL AND dma_privacy_scope.locality_value IS NULL ) OR ( ( CAST ( COALESCE ( (line_of_business = '21' or line_of_business LIKE '21u%' or line_of_business LIKE '%u21u%' or line_of_business LIKE '%u21') and (line_of_business NOT LIKE '%x%'), FALSE ) AS INT ) * cast ( pow ( 2, 21-1 ) as int ) + CAST ( COALESCE ( (line_of_business = '18' or line_of_business LIKE '18u%' or line_of_business LIKE '%u18u%' or line_of_business LIKE '%u18') and (line_of_business NOT LIKE '%x%'), FALSE ) AS INT ) * cast ( pow ( 2, 18-1 ) as int ) + CAST ( COALESCE ( (line_of_business = '13' or line_of_business LIKE '13u%' or line_of_business LIKE '%u13u%' or line_of_business LIKE '%u13') and (line_of_business NOT LIKE '%x%'), FALSE ) AS INT ) * cast ( pow ( 2, 13-1 ) as int ) ) = -1 OR ( COALESCE( ( CAST ( COALESCE ( (line_of_business = '21' or line_of_business LIKE '21u%' or line_of_business LIKE '%u21u%' or line_of_business LIKE '%u21') and (line_of_business NOT LIKE '%x%'), FALSE ) AS INT ) * cast ( pow ( 2, 21-1 ) as int ) + CAST ( COALESCE ( (line_of_business = '18' or line_of_business LIKE '18u%' or line_of_business LIKE '%u18u%' or line_of_business LIKE '%u18') and (line_of_business NOT LIKE '%x%'), FALSE ) AS INT ) * cast ( pow ( 2, 18-1 ) as int ) ), 0) = 0 AND dma_customer_consent.can_cross_use_all_data = 'Y' ) ) ) )
        """.trimIndent()
        val first = fmt(input)
        val second = fmt(first)
        assertEquals(first, second, "Formatting the monster query should be idempotent")
        assertTrue(first.lines().size > 10, "Monster query should break across many lines")
        assertTrue(
            first.lines().any { it.startsWith("LEFT JOIN") },
            "JOINs should be on their own lines"
        )
        assertTrue(
            first.lines().any { it.trimStart().startsWith("AND ") },
            "AND should break within ON clauses"
        )
    }
}
