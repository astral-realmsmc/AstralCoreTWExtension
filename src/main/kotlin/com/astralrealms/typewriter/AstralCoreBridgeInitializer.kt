package com.astralrealms.typewriter

import com.astralrealms.core.placeholder.container.RootPlaceholderContainer
import com.astralrealms.core.platform.action.PlatformActionRegistry
import com.astralrealms.core.platform.function.PlatformFunctionRegistry
import com.astralrealms.core.platform.requirement.PlatformRequirementRegistry
import com.astralrealms.typewriter.astral.TriggerTypewriterAction
import com.astralrealms.typewriter.astral.TypewriterFactFunction
import com.astralrealms.typewriter.astral.TypewriterFactRequirement
import com.astralrealms.typewriter.astral.TypewriterPlaceholder
import com.astralrealms.typewriter.internal.AstralCoreAccess
import com.typewritermc.core.extension.Initializable
import com.typewritermc.core.extension.annotations.Singleton
import org.bukkit.entity.Player

/**
 * Registers the AstralCore half of the bridge: the pieces that let an AstralCore configuration —
 * a menu, a dialog, an item — reach into Typewriter.
 *
 * The Typewriter half needs no registration; its entries are discovered from the extension jar.
 *
 * Everything is registered globally rather than against one plugin, so any AstralCore plugin on the
 * server can use it, not only AstralCore itself.
 */
@Singleton
class AstralCoreBridgeInitializer : Initializable {

    override suspend fun initialize() {
        if (AstralCoreAccess.plugin() == null) {
            AstralCoreAccess.logger.severe(
                "AstralCore is not enabled — the Typewriter bridge will not do anything. " +
                    "Install AstralCore, or remove this extension."
            )
            return
        }

        PlatformActionRegistry.get<Player>().registerGlobally(ACTION_NAME, TriggerTypewriterAction::class.java)
        PlatformRequirementRegistry.get<Player>().registerGlobally(REQUIREMENT_NAME, TypewriterFactRequirement::class.java)
        PlatformFunctionRegistry.get<Player>().registerGlobally(FUNCTION_NAME, TypewriterFactFunction::class.java)
        RootPlaceholderContainer.get().registerPlaceholder(TypewriterPlaceholder)

        AstralCoreAccess.logger.info(
            "Bridged Typewriter into AstralCore: [$ACTION_NAME] action, [$REQUIREMENT_NAME] requirement, " +
                "\$$FUNCTION_NAME() function and %${TypewriterPlaceholder.NAMESPACE}_...% placeholders."
        )
    }

    override suspend fun shutdown() {
        PlatformActionRegistry.get<Player>().unregister(ACTION_NAME)
        PlatformRequirementRegistry.get<Player>().unregister(REQUIREMENT_NAME)
        PlatformFunctionRegistry.get<Player>().unregister(FUNCTION_NAME)
        RootPlaceholderContainer.get().unregisterPlaceholder(TypewriterPlaceholder.NAMESPACE)

        // Parsed actions and requirements hold the AstralCore plugin instance they were built
        // against, which must not survive into the next load.
        AstralCoreAccess.clearCaches()
    }

    companion object {
        private const val ACTION_NAME = "typewriter"
        private const val REQUIREMENT_NAME = "typewriter-fact"
        private const val FUNCTION_NAME = "tw-fact"
    }
}
