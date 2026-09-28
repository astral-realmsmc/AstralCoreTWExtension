package com.astralrealms.typewriter.astral

import com.astralrealms.core.paper.model.requirement.PaperRequirement
import com.astralrealms.core.paper.model.requirement.PaperRequirementContext
import com.astralrealms.core.placeholder.wrapper.PlaceholderWrapper
import com.astralrealms.typewriter.internal.TypewriterAccess

/**
 * `[typewriter-fact] <fact id or name> [operator] [value]` — compares a Typewriter fact.
 *
 * With no operator the fact is simply required to be non-zero, which reads the way a quest flag is
 * usually meant: `[typewriter-fact] talked_to_elder`.
 */
// The parameters are nullable because AstralCore builds this reflectively from the configuration
// line, and the short form `[typewriter-fact] some_fact` supplies only the first one.
class TypewriterFactRequirement(
    private val fact: PlaceholderWrapper<String>?,
    private val operator: PlaceholderWrapper<String>?,
    private val value: PlaceholderWrapper<String>?,
) : PaperRequirement {

    override fun placeholderWrappers(): List<PlaceholderWrapper<*>> = listOfNotNull(fact, operator, value)

    override fun run(context: PaperRequirementContext): Boolean {
        val player = context.executor() ?: return false
        val id = fact?.get(context.parser())?.trim().orEmpty()
        if (id.isEmpty()) return false

        val actual = TypewriterAccess.fact(player, id) ?: return false
        val comparison = operator?.get(context.parser())?.trim().orEmpty()
        if (comparison.isEmpty()) return actual != 0

        val expected = value?.get(context.parser())?.trim()?.toIntOrNull() ?: return false
        return when (comparison) {
            "=", "==" -> actual == expected
            "!=", "<>" -> actual != expected
            ">" -> actual > expected
            ">=" -> actual >= expected
            "<" -> actual < expected
            "<=" -> actual <= expected
            else -> false
        }
    }
}
