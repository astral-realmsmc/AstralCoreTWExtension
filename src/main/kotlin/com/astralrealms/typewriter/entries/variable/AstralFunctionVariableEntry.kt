package com.astralrealms.typewriter.entries.variable

import com.astralrealms.typewriter.internal.AstralCoreAccess
import com.astralrealms.typewriter.internal.coerce
import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.engine.paper.entry.entries.VarContext
import com.typewritermc.engine.paper.entry.entries.VariableEntry

@Entry(
    "astral_function",
    "Calls a registered AstralCore function",
    Colors.GREEN,
    "mdi:code-tags",
)
/**
 * A variable that calls one AstralCore function by name — the same functions an AstralCore
 * configuration reaches with `$name(...)`, such as `round`, `min`, `max`, `format-number`,
 * `uppercase` or `apply-transformer`.
 *
 * Arguments are given as written in a configuration, so they may themselves contain placeholders
 * and nested calls.
 *
 * ## How could this be used?
 *
 * Format a number for a dialogue line, clamp a reward, or run one of your own registered functions
 * without having to express the call as a placeholder string.
 */
class AstralFunctionVariableEntry(
    override val id: String = "",
    override val name: String = "",
    @Help("The registered function name, without the leading '$' or the brackets.")
    val function: String = "",
    @Help("The arguments to pass, in order.")
    val arguments: List<String> = emptyList(),
) : VariableEntry {
    override fun <T : Any> get(context: VarContext<T>): T =
        context.coerce(AstralCoreAccess.callFunction(context.player, function, arguments))
}
