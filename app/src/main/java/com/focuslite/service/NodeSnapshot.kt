package com.focuslite.service

import android.view.accessibility.AccessibilityNodeInfo
import com.focuslite.domain.rules.UiNode

/**
 * Converts Android's live node tree into our small UiNode tree.
 *
 * Privacy: we copy only view id, class name, selection/focus flags and a short content
 * description. We never copy node.text (messages, captions), and we skip descriptions
 * on password fields entirely.
 *
 * Cost: every getChild() call may be a cross-process call into the target app, so the
 * tree size and depth are capped.
 */
object NodeSnapshot {
    private const val MAX_NODES = 1500
    private const val MAX_DEPTH = 60
    private const val MAX_DESC = 48

    fun from(root: AccessibilityNodeInfo): UiNode {
        var count = 0

        fun build(node: AccessibilityNodeInfo, depth: Int): UiNode {
            count++
            val children = ArrayList<UiNode>()
            if (depth < MAX_DEPTH) {
                for (i in 0 until node.childCount) {
                    if (count >= MAX_NODES) break
                    val child = node.getChild(i) ?: continue
                    children.add(build(child, depth + 1))
                }
            }
            return UiNode(
                viewId = node.viewIdResourceName?.substringAfter(":id/"),
                className = node.className?.toString()?.substringAfterLast('.'),
                contentDesc = if (node.isPassword) null else node.contentDescription?.toString()?.take(MAX_DESC),
                isSelected = node.isSelected,
                isEditableFocused = node.isEditable && node.isFocused,
                children = children,
            )
        }

        return build(root, 0)
    }
}
