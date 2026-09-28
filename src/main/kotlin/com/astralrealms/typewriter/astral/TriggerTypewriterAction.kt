package com.astralrealms.typewriter.astral

import com.astralrealms.core.paper.model.action.PaperAction
import com.astralrealms.core.paper.model.action.PaperActionContext
import com.astralrealms.core.placeholder.wrapper.PlaceholderWrapper
import com.astralrealms.typewriter.internal.AstralCoreAccess
import com.astralrealms.typewriter.internal.TypewriterAccess

/**
 * `[typewriter] <entry id or name>` — triggers a Typewriter entry for the executing player.
 *
 * Usable anywhere AstralCore runs actions: a menu item click, a dialog button, an item ability.
 */
// The parameter is nullable because AstralCore builds this reflectively from the configuration
// line: a `[typewriter]` written with no argument hands the constructor nothing at all.
class TriggerTypewriterAction(private val entry: PlaceholderWrapper<String>?) : PaperAction {

    override fun run(context: PaperActionContext) {
        val player = context.executor() ?: return
        val idOrName = entry?.get(context.parser())?.trim().orEmpty()
        if (idOrName.isEmpty()) {
            AstralCoreAccess.logger.warning("[typewriter] action is missing an entry id or name")
            return
        }
        if (!TypewriterAccess.trigger(player, idOrName)) {
            AstralCoreAccess.logger.warning("[typewriter] action could not find a triggerable entry '$idOrName'")
        }
    }
}
