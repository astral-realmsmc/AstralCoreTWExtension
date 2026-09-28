package com.astralrealms.typewriter.internal

import com.typewritermc.core.utils.ultraSafeCast
import com.typewritermc.engine.paper.entry.entries.VarContext
import kotlin.math.roundToInt

/** Reads [this] as a whole number, falling back to `0` — facts are integers and must always have one. */
internal fun String?.toIntOrZero(): Int {
    val raw = this?.trim().orEmpty()
    if (raw.isEmpty()) return 0
    raw.toIntOrNull()?.let { return it }
    raw.toDoubleOrNull()?.let { return it.roundToInt() }
    return when (raw.lowercase()) {
        "true", "yes" -> 1
        else -> 0
    }
}

/**
 * Converts a value AstralCore produced into the type the Typewriter field being filled expects.
 *
 * Typewriter's own [ultraSafeCast] covers strings and the numeric types; booleans are not in it, so
 * they are handled here — an AstralCore requirement or expression that reads as `true`, `yes` or a
 * non-zero number is true.
 */
internal fun <T : Any> VarContext<T>.coerce(value: Any?): T {
    if (klass == Boolean::class) {
        @Suppress("UNCHECKED_CAST")
        return value.asBoolean() as T
    }
    return klass.ultraSafeCast(value)
        ?: throw ClassCastException(
            "AstralCore returned '$value' for ${player.name}, which cannot be read as ${klass.qualifiedName}"
        )
}

private fun Any?.asBoolean(): Boolean = when (this) {
    null -> false
    is Boolean -> this
    is Number -> toDouble() != 0.0
    else -> when (toString().trim().lowercase()) {
        "true", "yes", "on" -> true
        "false", "no", "off", "" -> false
        else -> toString().trim().toDoubleOrNull()?.let { it != 0.0 } ?: false
    }
}
