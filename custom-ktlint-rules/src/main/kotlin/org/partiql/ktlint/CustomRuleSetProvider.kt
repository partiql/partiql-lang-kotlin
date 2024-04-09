package org.partiql.ktlint

import com.pinterest.ktlint.core.RuleSet
import com.pinterest.ktlint.core.RuleSetProvider
import org.partiql.ktlint.rule.TopLevelInternalRule
import org.partiql.ktlint.rule.TopLevelPublicRule

class CustomRuleSetProvider : RuleSetProvider {
    override fun get(): RuleSet = RuleSet("custom", TopLevelInternalRule(), TopLevelPublicRule())
}
