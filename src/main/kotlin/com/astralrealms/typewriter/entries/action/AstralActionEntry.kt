package com.astralrealms.typewriter.entries.action

import com.astralrealms.typewriter.internal.AstralCoreAccess
import com.typewritermc.core.books.pages.Colors
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.extension.annotations.Entry
import com.typewritermc.core.extension.annotations.Help
import com.typewritermc.engine.paper.entry.Criteria
import com.typewritermc.engine.paper.entry.Modifier
import com.typewritermc.engine.paper.entry.TriggerableEntry
import com.typewritermc.engine.paper.entry.entries.ActionEntry
import com.typewritermc.engine.paper.entry.entries.ActionTrigger

@Entry(
    "astral_action",
    "Runs AstralCore actions for the player",
    Colors.RED,
    "material-symbols:touch-app-rounded",
)
/**
 * The `Astral Action` entry runs a list of AstralCore actions for the interacting player.
 *
 * Each line is written exactly as it would be in an AstralCore menu, dialog or item — the action
 * type in brackets, its parameter, and any action properties:
 *
 * ```
 * [message] <gradient:#ffb347:#ffcc33>Welcome back, %player_name%!</gradient>
 * [sound] entity.player.levelup
 * [console-command] <delay=20> give %player_name% diamond 1
 * [open-menu] <if=[permission] astral.vip> vip_shop
 * ```
 *
 * ## How could this be used?
 *
 * Reuse the actions your menus and dialogs already use inside a Typewriter dialogue, so a quest
 * reward, a toast or a menu opening is defined once and behaves the same wherever it is triggered.
 */
class AstralActionEntry(
    override val id: String = "",
    override val name: String = "",
    override val criteria: List<Criteria> = emptyList(),
    override val modifiers: List<Modifier> = emptyList(),
    override val triggers: List<Ref<TriggerableEntry>> = emptyList(),
    @Help("The AstralCore action lines to run, in order.")
    val actions: List<String> = emptyList(),
) : ActionEntry {
    override fun ActionTrigger.execute() {
        AstralCoreAccess.runActions(player, actions)
    }
}
