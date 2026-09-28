package com.astralrealms.typewriter.entries.fact

import com.astralrealms.typewriter.internal.AstralCoreAccess
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
    "astral_requirement_fact",
    "A fact that is 1 when the player meets AstralCore requirements",
    Colors.PURPLE,
    "material-symbols:flag",
)
/**
 * A read-only [fact](/docs/creating-stories/facts) that is `1` when the player meets every
 * AstralCore requirement listed, and `0` otherwise.
 *
 * ## How could this be used?
 *
 * Criteria are how Typewriter gates dialogue, quests and objectives, so turning an AstralCore
 * requirement into a fact makes it usable everywhere a criterion is: `= 1` to require it, `= 0` to
 * exclude it.
 */
class AstralRequirementFactEntry(
    override val id: String = "",
    override val name: String = "",
    override val comment: String = "",
    override val group: Ref<GroupEntry> = emptyRef(),
    @Help("The AstralCore requirements to check.")
    val requirements: List<String> = emptyList(),
) : ReadableFactEntry {
    override fun readSinglePlayer(player: Player): FactData =
        FactData(if (AstralCoreAccess.testRequirements(player, requirements)) 1 else 0)
}
