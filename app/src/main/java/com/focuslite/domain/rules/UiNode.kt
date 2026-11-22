package com.focuslite.domain.rules

/**
 * A tiny, privacy-reduced copy of one on-screen element.
 *
 * The accessibility service converts Android's AccessibilityNodeInfo tree into these objects.
 * The rule engine only ever sees UiNode, never Android classes, so it can be unit-tested on a
 * normal JVM with hand-written fake screens.
 *
 * Deliberately NOT included: the visible text of any element (messages, captions, usernames).
 */
data class UiNode(
    /** Short resource id, e.g. "clips_tab" (the "com.instagram.android:id/" prefix is stripped). */
    val viewId: String? = null,
    /** Short class name, e.g. "FrameLayout". */
    val className: String? = null,
    /** Accessibility label (truncated). Used mainly for tab labels like "Reels". Never for password fields. */
    val contentDesc: String? = null,
    val isSelected: Boolean = false,
    /** True when this is a text field that currently has focus (user is typing). */
    val isEditableFocused: Boolean = false,
    val children: List<UiNode> = emptyList(),
) {
    /** Depth-first list of this node and all descendants. */
    fun flatten(): List<UiNode> {
        val out = ArrayList<UiNode>()
        val stack = ArrayDeque<UiNode>()
        stack.addLast(this)
        while (stack.isNotEmpty()) {
            val n = stack.removeLast()
            out.add(n)
            for (i in n.children.indices.reversed()) stack.addLast(n.children[i])
        }
        return out
    }
}
