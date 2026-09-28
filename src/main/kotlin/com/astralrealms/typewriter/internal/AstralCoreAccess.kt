package com.astralrealms.typewriter.internal

import com.astralrealms.core.paper.AstralPaperAPI
import com.astralrealms.core.paper.model.action.PaperActionFactory
import com.astralrealms.core.paper.model.action.PaperActionList
import com.astralrealms.core.paper.model.action.impl.DefaultPaperActionContext
import com.astralrealms.core.paper.model.function.PaperFunctionFactory
import com.astralrealms.core.paper.model.requirement.PaperRequirementFactory
import com.astralrealms.core.paper.model.requirement.PaperRequirementList
import com.astralrealms.core.paper.model.requirement.impl.DefaultPaperRequirementContext
import com.astralrealms.core.paper.plugin.AstralPaperPlugin
import com.astralrealms.core.placeholder.container.PlaceholderContainer
import com.astralrealms.core.placeholder.wrapper.PlaceholderWrapper
import com.astralrealms.core.placeholder.wrapper.PlaceholderWrappers
import com.astralrealms.core.platform.function.PreparedFunction
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Everything this extension needs from the AstralCore Paper module.
 *
 * AstralCore parses an action, a requirement or an inline function once and then runs the result
 * many times — building one is not cheap, since it reflects over a constructor and compiles any
 * `$e(...)` expression in the arguments. Typewriter entries are required to be stateless, so the
 * parsed forms are cached here, keyed by the configuration they were written as.
 */
object AstralCoreAccess {

    const val PLUGIN_NAME: String = "AstralCore"

    val logger: Logger = Logger.getLogger("Typewriter/AstralCore")

    private val actionLists = ConcurrentHashMap<List<String>, Holder<PaperActionList>>()
    private val requirementLists = ConcurrentHashMap<List<String>, Holder<PaperRequirementList>>()
    private val wrappers = ConcurrentHashMap<String, Holder<PlaceholderWrapper<String>>>()
    private val functions = ConcurrentHashMap<FunctionCall, PreparedFunction>()

    /** Holds a nullable value so a failed parse is cached too, instead of retried (and re-logged) forever. */
    private class Holder<T : Any>(val value: T?)

    private data class FunctionCall(val name: String, val arguments: List<String>)

    /** The running AstralCore plugin, or `null` when it is not installed or not enabled yet. */
    fun plugin(): AstralPaperPlugin? =
        Bukkit.getPluginManager().getPlugin(PLUGIN_NAME) as? AstralPaperPlugin

    private fun requirePlugin(): AstralPaperPlugin? {
        val plugin = plugin()
        if (plugin == null) {
            logger.warning("AstralCore is not available, skipping. Is the plugin installed and enabled?")
        }
        return plugin
    }

    /** A placeholder container bound to [player], which is also the parser AstralCore contexts take. */
    fun parser(player: Player?): PlaceholderContainer = AstralPaperAPI.createPlaceholderContainer(player)

    // -----------------------------------------------------------------------------------------
    // Actions
    // -----------------------------------------------------------------------------------------

    /** Runs AstralCore action lines such as `[message] <red>Hello %player_name%` for [player]. */
    fun runActions(player: Player, lines: List<String>, extraContexts: Collection<Any> = emptyList()) {
        val actions = actionList(lines) ?: return
        val context = DefaultPaperActionContext(player, extraContexts, parser(player))
        try {
            actions.run(context)
        } catch (throwable: Throwable) {
            logger.log(Level.SEVERE, "Failed to run AstralCore actions $lines for ${player.name}", throwable)
        }
    }

    private fun actionList(lines: List<String>): PaperActionList? {
        if (lines.isEmpty()) return null
        return actionLists.computeIfAbsent(lines) { key ->
            val plugin = requirePlugin() ?: return@computeIfAbsent Holder(null)
            try {
                Holder(PaperActionFactory.instance().createList(plugin, key))
            } catch (throwable: Throwable) {
                logger.log(Level.SEVERE, "Failed to parse AstralCore actions $key", throwable)
                Holder(null)
            }
        }.value
    }

    // -----------------------------------------------------------------------------------------
    // Requirements
    // -----------------------------------------------------------------------------------------

    /**
     * Whether [player] meets every AstralCore requirement in [lines].
     *
     * An empty list is met by definition, and a list that cannot be parsed is not — a broken
     * requirement should close a door, never open one.
     */
    fun testRequirements(player: Player, lines: List<String>, extraContexts: Collection<Any> = emptyList()): Boolean {
        if (lines.isEmpty()) return true
        val requirements = requirementList(lines) ?: return false
        val context = DefaultPaperRequirementContext(player, extraContexts, parser(player))
        return try {
            requirements.run(context)
        } catch (throwable: Throwable) {
            logger.log(Level.SEVERE, "Failed to check AstralCore requirements $lines for ${player.name}", throwable)
            false
        }
    }

    private fun requirementList(lines: List<String>): PaperRequirementList? =
        requirementLists.computeIfAbsent(lines) { key ->
            val plugin = requirePlugin() ?: return@computeIfAbsent Holder(null)
            try {
                Holder(PaperRequirementFactory.instance().createList(plugin, key))
            } catch (throwable: Throwable) {
                logger.log(Level.SEVERE, "Failed to parse AstralCore requirements $key", throwable)
                Holder(null)
            }
        }.value

    // -----------------------------------------------------------------------------------------
    // Placeholders, inline functions and expressions
    // -----------------------------------------------------------------------------------------

    /**
     * Resolves [raw] against AstralCore for [player]: `%placeholders%`, inline `$function(...)`
     * calls and `$e(...)` expressions all go through the same wrapper AstralCore uses itself.
     *
     * Falls back to [raw] when it cannot be resolved, the way an unresolved placeholder already
     * renders as itself — a broken expression should be visible, not blank.
     */
    fun parse(player: Player?, raw: String): String {
        if (raw.isEmpty()) return raw
        val wrapper = wrapper(raw) ?: return raw
        return try {
            wrapper.get(parser(player)) ?: raw
        } catch (throwable: Throwable) {
            logger.log(Level.SEVERE, "Failed to parse '$raw' for ${player?.name ?: "console"}", throwable)
            raw
        }
    }

    private fun wrapper(raw: String): PlaceholderWrapper<String>? =
        wrappers.computeIfAbsent(raw) { key ->
            try {
                Holder(PlaceholderWrappers.wrap(key, String::class.java))
            } catch (throwable: Throwable) {
                logger.log(Level.SEVERE, "Failed to wrap '$key' as an AstralCore placeholder", throwable)
                Holder(null)
            }
        }.value

    /** Calls a registered AstralCore function, e.g. `round` with `["%some_number%", "2"]`. */
    fun callFunction(player: Player?, name: String, arguments: List<String>): Any? {
        if (name.isEmpty()) return null
        if (plugin() == null) {
            requirePlugin()
            return null
        }
        val prepared = functions.computeIfAbsent(FunctionCall(name, arguments)) { call ->
            PaperFunctionFactory.instance().prepare(call.name, call.arguments)
        }
        return try {
            prepared.compute(parser(player))
        } catch (throwable: Throwable) {
            logger.log(Level.SEVERE, "Failed to run AstralCore function '$name' with $arguments", throwable)
            null
        }
    }

    /** Drops every parsed form. Called on shutdown so a reload does not keep a stale plugin instance. */
    fun clearCaches() {
        actionLists.clear()
        requirementLists.clear()
        wrappers.clear()
        functions.clear()
    }
}
