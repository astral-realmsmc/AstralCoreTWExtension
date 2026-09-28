package com.astralrealms.typewriter.entries.variable

import com.astralrealms.typewriter.internal.AstralCoreAccess
import com.astralrealms.typewriter.internal.coerce
import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.core.extension.annotations.VariableData
import com.typewritermc.engine.paper.entry.PlaceholderEntry
import com.typewritermc.engine.paper.entry.PlaceholderParser
import com.typewritermc.engine.paper.entry.placeholderParser
import com.typewritermc.engine.paper.entry.entries.VarContext
import com.typewritermc.engine.paper.entry.entries.VariableEntry
import com.typewritermc.engine.paper.entry.entries.getData
import com.typewritermc.engine.paper.entry.supply

@Entry(
    "astral_placeholder",
    "Reads an AstralCore placeholder, function or expression",
    Colors.GREEN,
    "mdi:application-variable",
)
@VariableData(AstralPlaceholderVariableData::class)
/**
 * A variable whose value comes from AstralCore's placeholder engine. It understands everything an
 * AstralCore configuration string does — `%placeholders%` (including PlaceholderAPI ones), inline
 * `$function(...)` calls and `$e(...)` expressions:
 *
 * ```
 * %player_name%
 * $uppercase(%astral_rank%)
 * $e(%astral_level% * 100)
 * ```
 *
 * Each place the variable is used may override the expression, so one entry can serve many fields.
 * The result is converted to whatever the field expects — text, a number or a boolean.
 *
 * ## How could this be used?
 *
 * Put a live value from your core plugins into any Typewriter field: a player's rank in a dialogue
 * line, a computed reward amount in an action, a balance in a sidebar.
 */
class AstralPlaceholderVariableEntry(
    override val id: String = "",
    override val name: String = "",
    @Help("The AstralCore placeholder, function or expression to resolve. Can be overridden per usage.")
    val value: String = "",
) : VariableEntry, PlaceholderEntry {

    private fun expression(data: AstralPlaceholderVariableData?): String =
        data?.value?.takeIf { it.isNotBlank() } ?: value

    override fun <T : Any> get(context: VarContext<T>): T =
        context.coerce(AstralCoreAccess.parse(context.player, expression(context.getData())))

    override fun parser(): PlaceholderParser = placeholderParser {
        supply { player -> player?.let { AstralCoreAccess.parse(it, value) } }
    }
}

class AstralPlaceholderVariableData(
    @Help("Overrides the entry's expression for this usage only.")
    val value: String = "",
)
