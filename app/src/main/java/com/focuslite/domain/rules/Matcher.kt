package com.focuslite.domain.rules

/** Describes one on-screen element we are looking for. Null fields mean "don't care". */
data class NodeSpec(
    val viewId: String? = null,
    val descPrefix: String? = null,
    val selected: Boolean? = null,
    val editableFocused: Boolean? = null,
) {
    fun test(n: UiNode): Boolean =
        (viewId == null || n.viewId == viewId) &&
            (descPrefix == null || n.contentDesc?.startsWith(descPrefix, ignoreCase = true) == true) &&
            (selected == null || n.isSelected == selected) &&
            (editableFocused == null || n.isEditableFocused == editableFocused)
}

/** A condition on a whole screen (a flattened node list). Composable like boolean logic. */
sealed interface Matcher {
    fun matches(nodes: List<UiNode>): Boolean
}

data class Exists(val spec: NodeSpec) : Matcher {
    override fun matches(nodes: List<UiNode>) = nodes.any(spec::test)
}

data class AnyOf(val items: List<Matcher>) : Matcher {
    override fun matches(nodes: List<UiNode>) = items.any { it.matches(nodes) }
}

data class AllOf(val items: List<Matcher>) : Matcher {
    override fun matches(nodes: List<UiNode>) = items.all { it.matches(nodes) }
}

data class Not(val item: Matcher) : Matcher {
    override fun matches(nodes: List<UiNode>) = !item.matches(nodes)
}

// --- Small DSL so rule files read clearly ---
fun viewId(id: String, selected: Boolean? = null) = Exists(NodeSpec(viewId = id, selected = selected))
fun descStartsWith(prefix: String, selected: Boolean? = null) = Exists(NodeSpec(descPrefix = prefix, selected = selected))
fun typingInTextField() = Exists(NodeSpec(editableFocused = true))
fun anyOf(vararg m: Matcher) = AnyOf(m.toList())
fun allOf(vararg m: Matcher) = AllOf(m.toList())
fun not(m: Matcher) = Not(m)
