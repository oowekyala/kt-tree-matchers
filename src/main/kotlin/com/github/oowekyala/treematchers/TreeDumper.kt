package com.github.oowekyala.treematchers

/**
 * Dumps a node of the hierarchy [H] to its DSL node spec.
 * Customize it to your liking. By default only dumps the
 * structure of the subtree,
 *
 *
 * @author Clément Fournier
 * @since 1.0
 */
open class TreeDumper<H : Any>(
        /** Adapter for the [H] hierarchy. */
        protected val adapter: TreeLikeAdapter<H>,
        /** Size of each indent level. */
        protected val indentSize: Int = 4
) {
    /**
     * Returns a list of strings that will be added to the nodeSpec dump of [node].
     * E.g. if [node] has the properties `prefix` and `suffix`,
     * which should be asserted to be `"pre"` and `"fix"`, respectively,
     * then the returned list should contain the strings `it.prefix shouldBe "pre"`
     * and `it.suffix shouldBe "fix"`. The actual format of assertions depends on your
     * testing style, which is why they are left for you to decide.
     *
     * By default, no additional assertions will be added.
     *
     * @param node Node to get the assertions for.
     */
    open fun getAdditionalAssertions(node: H): List<String> = emptyList()

    /**
     * Returns a map of assertions that will be appended by a [TreeNodeWrapper.child]
     * nodespec dump. E.g. if [node] has a property "firstChild" which should
     * be equal to the first child of [node], then the returned map should contain
     * a mapping `0 -> "it.firstChild shouldBe "`, which will be translated to
     *
     *     it.firstChild shouldBe child<FirstChildActualType> {
     *       ...
     *     }
     *
     * Don't forget to include some whitespace at the end of the strings if needed.
     *
     * Children which don't have a corresponding child assertion will still
     * see their `child` dumped. Mappings from an index that doesn't exist
     * will be ignored.
     *
     * By default, no child assertion is added, which means all `child` calls
     * are inserted with no prefix.
     *
     * @param node Node to get the assertions for.
     *
     */
    open fun getChildAssertions(node: H): Map<Int, String> = emptyMap()

    /**
     * Returns the string that will be prepended to the opening brace
     * of the root's spec. Called with [node] as the root node.
     *
     * By default this is [getRootMatchingMethodName] plus [getRootMatchingMethodArguments].
     *
     * @param node Root of the tree
     * @param shouldOnlyDumpRoot Whether only the root should be dumped (because of maxDumpDepth = 0
     * on [dumpSubtree]), in which case you may want to add a parameter like `(ignoreChildren = true)`
     *
     */
    open fun getRootSpecPrefix(node: H, shouldOnlyDumpRoot: Boolean) = getRootMatchingMethodName(node) + getRootMatchingMethodArguments(node, shouldOnlyDumpRoot)

    /**
     * Returns the parameters and type arguments that will be
     * appended to [getRootMatchingMethodName] in the default
     * implementation of [getRootSpecPrefix].
     *
     * By default this is `"<${node.javaClass.simpleName}>"`,
     * plus `"(ignoreChildren = true)"` if [shouldOnlyDumpRoot] is true.
     *
     * You should configure with if the arguments of your own matching method
     * if they don't match the defaults.
     *
     * @param node Root of the tree
     * @param shouldOnlyDumpRoot Whether only the root should be dumped (because of maxDumpDepth = 0
     * on [dumpSubtree]), in which case you may want to add a parameter like `(ignoreChildren = true)`
     *
     */
    open fun getRootMatchingMethodArguments(node: H, shouldOnlyDumpRoot: Boolean): String =
            "<${node.javaClass.simpleName}>" +
            if (shouldOnlyDumpRoot) "(ignoreChildren = true)" else ""

    /**
     * Returns the name of the root matching method. This is used
     * in the default version of [getRootSpecPrefix], and allows you
     * to keep the default version of [getRootMatchingMethodArguments]
     * but just change the method name. By default this is `"matchNode"`.
     */
    open fun getRootMatchingMethodName(node: H) = "matchNode"

    /**
     * Dumps the given subtree to its DSL node spec.
     *
     * @param node Subtree to dump
     * @param maxDumpDepth Maximum depth on which to recurse. A value of zero only dumps the root node.
     * A negative value dumps the whole subtree.
     */
    fun dumpSubtree(node: H, maxDumpDepth: Int = -1): String =
            StringBuilder().also {
                appendSubtree(node, it, false, 0, maxDumpDepth)
            }.toString()

    private fun appendSubtree(node: H, builder: StringBuilder, isChild: Boolean, indentDepth: Int, maxDumpDepth: Int) {
        val bodyIdentSize = indentDepth + indentSize

        val additionalAssertions = getAdditionalAssertions(node)
        val children = if (maxDumpDepth == 1) emptyList() else adapter.getChildren(node)
        val childAssertions = if (maxDumpDepth == 1) emptyMap() else getChildAssertions(node)


        if (isChild) {
            val params = if (maxDumpDepth == 1) "(ignoreChildren = true)" else ""

            builder.append("child<").append(node.javaClass.simpleName).append(">").append(params).append(" {")
        } else {
            // this is the root
            builder.append(getRootSpecPrefix(node, maxDumpDepth == 0)).append(" {")
        }

        for (assertion in additionalAssertions) {
            builder.appendNewLine(bodyIdentSize).append(assertion)
        }

        if (maxDumpDepth != 0) {

            children.forEachIndexed { i, child ->
                if (additionalAssertions.isNotEmpty() && i == 0 || childAssertions[i] != null) {
                    builder.appendNewLine(0)
                }
                builder.appendNewLine(bodyIdentSize)

                childAssertions[i]?.run {
                    builder.append(this)
                }

                appendSubtree(child, builder, true, bodyIdentSize, maxDumpDepth - 1)
            }
        }

        if (children.isNotEmpty() || additionalAssertions.isNotEmpty()) {
            builder.appendNewLine(indentDepth)
        }

        builder.append("}")
    }

    private fun StringBuilder.appendNewLine(currentIndent: Int): StringBuilder =
            append("\n").append(" ".repeat(currentIndent))
}

/**
 * A config object that decides how to format tree dumps on error messages.
 */
data class DumpConfig<H : Any>(val treeDumper: TreeDumper<H>, val maxDumpDepth: Int = -1) {

    fun dumpSubtree(node: H) = treeDumper.dumpSubtree(node, maxDumpDepth)

    companion object {
        fun <H : Any> default(adapter: TreeLikeAdapter<H>) = DumpConfig(TreeDumper(adapter))
    }
}