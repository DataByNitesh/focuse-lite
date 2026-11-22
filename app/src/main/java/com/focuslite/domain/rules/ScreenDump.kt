package com.focuslite.domain.rules

/**
 * Turns a UiNode tree into compact text for the Diagnostics screen, so screen structure can be
 * shared to update rules. Contains only class names, view ids, short labels and flags — no text.
 */
object ScreenDump {
    private const val MAX_DESC = 30

    fun format(root: UiNode): String {
        val sb = StringBuilder()
        fun visit(n: UiNode, depth: Int) {
            val interesting = n.viewId != null || !n.contentDesc.isNullOrBlank() || n.isSelected || n.isEditableFocused
            if (interesting) {
                sb.append("  ".repeat(depth.coerceAtMost(20)))
                sb.append(n.className ?: "?")
                n.viewId?.let { sb.append(" #").append(it) }
                n.contentDesc?.takeIf { it.isNotBlank() }?.let { sb.append(" [").append(it.take(MAX_DESC)).append(']') }
                if (n.isSelected) sb.append(" *selected")
                if (n.isEditableFocused) sb.append(" *typing")
                sb.append('\n')
            }
            n.children.forEach { visit(it, if (interesting) depth + 1 else depth) }
        }
        visit(root, 0)
        return sb.toString()
    }
}
