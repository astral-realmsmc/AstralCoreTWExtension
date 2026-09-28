package com.astralrealms.typewriter.entries.audience

import com.astralrealms.typewriter.internal.AstralCoreAccess
import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.ref
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.engine.paper.entry.entries.AudienceEntry
import com.typewritermc.engine.paper.entry.entries.AudienceFilter
import com.typewritermc.engine.paper.entry.entries.AudienceFilterEntry
import com.typewritermc.engine.paper.entry.entries.Invertible
import com.typewritermc.engine.paper.entry.entries.TickableDisplay
import org.bukkit.entity.Player

@Entry(
    "astral_requirement_audience",
    "Filters an audience on AstralCore requirements",
    Colors.MEDIUM_SEA_GREEN,
    "material-symbols:filter-alt",
)
/**
 * The `Astral Requirement Audience` entry only shows its children to players who meet every
 * AstralCore requirement listed, written exactly as in an AstralCore configuration:
 *
 * ```
 * [permission] astral.vip
 * [placeholder] %astral_level% >= 10
 * ```
 *
 * ## How could this be used?
 *
 * Show a sidebar, an NPC or a quest only to players who already satisfy a condition your core
 * configuration knows about, without re-modelling that condition as Typewriter facts.
 */
class AstralRequirementAudienceEntry(
    override val id: String = "",
    override val name: String = "",
    override val children: List<Ref<out AudienceEntry>> = emptyList(),
    @Help("The AstralCore requirements every player in the audience must meet.")
    val requirements: List<String> = emptyList(),
    @Help("How often, in ticks, the requirements are re-checked. Use 0 to only check when a player joins the audience.")
    val refreshInterval: Int = 20,
    override val inverted: Boolean = false,
) : AudienceFilterEntry, Invertible {
    override suspend fun display(): AudienceFilter =
        AstralRequirementAudienceFilter(ref(), requirements, refreshInterval)
}

class AstralRequirementAudienceFilter(
    ref: Ref<out AudienceFilterEntry>,
    private val requirements: List<String>,
    private val refreshInterval: Int,
) : AudienceFilter(ref), TickableDisplay {

    private var ticks = 0

    override fun filter(player: Player): Boolean = AstralCoreAccess.testRequirements(player, requirements)

    override fun tick() {
        if (refreshInterval <= 0) return
        if (++ticks < refreshInterval) return
        ticks = 0
        consideredPlayers.forEach { it.refresh() }
    }
}
