package com.astralrealms.typewriter.entries.fact

import com.astralrealms.typewriter.internal.AstralCoreAccess
import com.astralrealms.typewriter.internal.toIntOrZero
import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.engine.paper.entry.entries.GroupEntry
import com.typewritermc.engine.paper.entry.entries.ReadableFactEntry
import com.typewritermc.engine.paper.facts.FactData
import org.bukkit.entity.Player

@Entry(
    "astral_number_fact",
    "A fact read from an AstralCore placeholder or expression",
    Colors.PURPLE,
    "fa6-solid:hashtag",
)
/**
 * A read-only [fact](/docs/creating-stories/facts) whose value is an AstralCore placeholder,
 * inline function or expression resolved for the player and rounded to a whole number:
 *
 * ```
 * %astral_level%
 * $e(%vault_eco_balance% / 100)
 * $round(%astral_progress%)
 * ```
 *
 * Anything that does not resolve to a number reads as `0`.
 *
 * ## How could this be used?
 *
 * Compare a value your core plugins already expose — a level, a balance, a stat — inside Typewriter
 * criteria, without writing that value into Typewriter's own fact storage.
 */
class AstralNumberFactEntry(
    override val id: String = "",
    override val name: String = "",
    override val comment: String = "",
    override val group: Ref<GroupEntry> = emptyRef(),
    @Help("The AstralCore placeholder, function or expression to read.")
    val value: String = "",
) : ReadableFactEntry {
    override fun readSinglePlayer(player: Player): FactData =
        FactData(AstralCoreAccess.parse(player, value).toIntOrZero())
}
