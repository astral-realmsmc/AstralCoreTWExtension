package com.astralrealms.typewriter.internal

import com.typewritermc.core.entries.Query
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.interaction.context
import com.typewritermc.engine.paper.entry.PlaceholderEntry
import com.typewritermc.engine.paper.entry.TriggerableEntry
import com.typewritermc.engine.paper.entry.entries.ReadableFactEntry
import com.typewritermc.engine.paper.entry.triggerFor
import com.typewritermc.engine.paper.interaction.interactionContext
import org.bukkit.Bukkit
import org.bukkit.entity.Player

/**
 * Everything the AstralCore side of the bridge needs from Typewriter.
 *
 * Entries are addressed by id first and by name second, because a page author reads the name while
 * the id is what survives a rename.
 */
object TypewriterAccess {

    /**
     * Triggers the Typewriter entry [idOrName] for [player], on the main thread.
     *
     * Returns whether the entry was found — an AstralCore action can be declared `<async=true>`,
     * in which case the lookup already happened off the main thread and only the trigger is posted.
     */
    fun trigger(player: Player, idOrName: String): Boolean {
        val entry = findTriggerable(idOrName) ?: return false
        val ref = Ref(entry.id, TriggerableEntry::class)
        onMain { ref.triggerFor(player, player.interactionContext ?: context()) }
        return true
    }

    private fun findTriggerable(idOrName: String): TriggerableEntry? =
        Query.findById<TriggerableEntry>(idOrName) ?: Query.findByName<TriggerableEntry>(idOrName)

    /** The value of the Typewriter fact [idOrName] for [player], or `null` when there is no such fact. */
    fun fact(player: Player, idOrName: String): Int? {
        val entry = Query.findById<ReadableFactEntry>(idOrName)
            ?: Query.findByName<ReadableFactEntry>(idOrName)
            ?: return null
        return entry.readForPlayersGroup(player).value
    }

    /**
     * Resolves a Typewriter placeholder body for [player], the part that would follow
     * `%typewriter_` — an entry id and any `:`-separated arguments, e.g. `my_fact:remaining:10`.
     */
    fun placeholder(player: Player, body: String): String? {
        val parts = body.split(':')
        val id = parts.firstOrNull()?.takeIf { it.isNotEmpty() } ?: return null
        val entry = Query.findById<PlaceholderEntry>(id) ?: Query.findByName<PlaceholderEntry>(id) ?: return null
        return entry.parser().parse(player, parts.drop(1))
    }

    private fun onMain(block: () -> Unit) {
        if (Bukkit.isPrimaryThread()) {
            block()
            return
        }
        val plugin = AstralCoreAccess.plugin() ?: return
        Bukkit.getScheduler().runTask(plugin, Runnable { block() })
    }
}
