package com.astralrealms.typewriter.astral

import com.astralrealms.core.placeholder.PlaceholderContext
import com.astralrealms.core.placeholder.impl.system.ComplexPlaceholder
import com.astralrealms.typewriter.internal.TypewriterAccess
import org.bukkit.entity.Player

/**
 * `%typewriter_<entry id>%`, and `%typewriter_<entry id>:<argument>%` for an entry that takes
 * arguments, resolved inside any AstralCore string.
 *
 * Typewriter exposes the same placeholders through PlaceholderAPI; registering them here means they
 * also resolve when PlaceholderAPI is not in the picture, and without a round trip through it.
 */
object TypewriterPlaceholder : ComplexPlaceholder {

    const val NAMESPACE: String = "typewriter"

    override fun namespace(): String = NAMESPACE

    override fun get(context: PlaceholderContext): Any? {
        val player = context.context() as? Player ?: return null
        val body = context.collapseRemaining("_") ?: return null
        return TypewriterAccess.placeholder(player, body)
    }
}
