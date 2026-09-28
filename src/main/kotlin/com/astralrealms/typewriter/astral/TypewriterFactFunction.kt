package com.astralrealms.typewriter.astral

import com.astralrealms.core.paper.model.function.PaperFunction
import com.astralrealms.core.paper.model.function.PaperFunctionContext
import com.astralrealms.core.placeholder.wrapper.PlaceholderWrapper
import com.astralrealms.typewriter.internal.TypewriterAccess

/**
 * `$tw-fact(<fact id or name>)` — the value of a Typewriter fact, as a number.
 *
 * Reads `0` when the fact does not exist or there is no player to read it for, so it stays usable
 * inside an `$e(...)` expression.
 */
class TypewriterFactFunction(private val fact: PlaceholderWrapper<String>?) : PaperFunction {

    override fun run(context: PaperFunctionContext): Any {
        val player = context.executor() ?: return 0
        val id = fact?.get(context.parser())?.trim().orEmpty()
        if (id.isEmpty()) return 0
        return TypewriterAccess.fact(player, id) ?: 0
    }
}
