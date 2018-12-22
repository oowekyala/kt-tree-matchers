package com.github.oowekyala.treeutils.printers

import com.github.oowekyala.treeutils.TreeLikeAdapter

/**
 * Dumps a node of the hierarchy [H] to its DSL node spec.
 * Customize it to your liking. By default only dumps the
 * structure of the subtree, without any assertions.
 * [JUnitBeanTreePrinter] or [KotlintestBeanTreePrinter] can
 * add assertions, which is especially useful if you want to
 * generate complete unit tests.
 *
 * @author Clément Fournier
 * @since 1.0
 */
open class DslStructurePrinter<H : Any>(
        /** Adapter for the [H] hierarchy. */
        protected val adapter: TreeLikeAdapter<H>,
        /** Size of each indent level. */
        protected val indentSize: Int = 4
) : TreePrinter<H> {

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
    protected open fun getAdditionalAssertions(node: H): List<String> = emptyList()

    /**
     * Returns a map of the contexts surrounding the nodespec dumps
     * corresponding to the children of this node.
     *
     * The mappings map a child index to a pairs (prefix, suffix) that should
     * surround the call to [TreeNodeWrapper.child] corresponding to the
     * child with the key child in the nodespec of the [parent] node.
     *
     *
     * E.g. if [parent] has a property "firstChild" which should
     * be equal to its first child, then the returned map could contain
     * a mapping `0 -> Pair("it.firstChild shouldBe ", "")`, which will be translated to
     *
     *     it.firstChild shouldBe child<FirstChildActualType> {
     *       ... // nodespec of the child
     *     }
     *
     * To get a JUnit like syntax you could have a mapping
     * `0 -> Pair("val child0 = ", "\n assertEquals(child0, it.firstChild)`,
     * which would be translated to
     *
     *     val child0 = child<FirstChildActualType> {
     *       ... // nodespec of the child
     *     }
     *     assertEquals(child0, it.firstChild)
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
     * @param parent Node to get the assertions for.
     *
     */
    protected open fun getChildCallContexts(parent: H): Map<Int, Pair<String, String>> = emptyMap()

    /**
     * Returns a custom assertion that should be used for the [child] in the [parent].
     * If not null, the returned string replaces the call to [TreeNodeWrapper.child],
     * and [getChildCallContexts] is ignored. If null then [getChildCallContexts] is
     * honored. Use this to e.g. replace an assertion for a subtree of a particular
     * form by a custom assertion method. Be aware that if the assertion returned
     * doesn't call [TreeNodeWrapper.child] on [parent] (which is implicit receiver
     * in the scope of the returned string) exactly once, then the structure will
     * be wrong.
     */
    protected open fun getCustomChildAssertion(parent: H, child: H): String? = null

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
    protected open fun getRootSpecPrefix(node: H, shouldOnlyDumpRoot: Boolean) = getRootMatchingMethodName(node) + getRootMatchingMethodArguments(node, shouldOnlyDumpRoot)

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
    protected open fun getRootMatchingMethodArguments(node: H, shouldOnlyDumpRoot: Boolean): String =
            "<${node.javaClass.simpleName}>" +
            if (shouldOnlyDumpRoot) "(ignoreChildren = true)" else ""

    /**
     * Returns the name of the root matching method. This is used
     * in the default version of [getRootSpecPrefix], and allows you
     * to keep the default version of [getRootMatchingMethodArguments]
     * but just change the method name. By default this is `"node"`.
     */
    protected open fun getRootMatchingMethodName(node: H) = "node"

    /**
     * Dumps the given subtree to its DSL node spec.
     *
     * @param node Subtree to dump
     * @param maxDumpDepth Maximum depth on which to recurse. A value of zero only dumps the root node.
     * A negative value dumps the whole subtree.
     */
    override fun dumpSubtree(
            node: H,
            maxDumpDepth: Int
    ): String =
            StringBuilder().also {
                appendSubtree(node, it, false, 0, maxDumpDepth)
            }.toString()

    private fun appendSubtree(node: H, builder: StringBuilder, isChild: Boolean, indentDepth: Int, maxDumpDepth: Int) {
        val bodyIdentSize = indentDepth + indentSize

        val additionalAssertions = getAdditionalAssertions(node)
        val children = if (maxDumpDepth == 0) emptyList() else adapter.getChildren(node)
        val childAssertions = if (maxDumpDepth == 0) emptyMap() else getChildCallContexts(node)


        if (isChild) {
            val params = if (maxDumpDepth == 0) "(ignoreChildren = true)" else ""

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

                fun appendExternal(string: String) {
                    for (it in string) {
                        when (it) {
                            '\n' -> builder.appendNewLine(bodyIdentSize)
                            else -> builder.append(it)
                        }
                    }
                }

                getCustomChildAssertion(node, child)?.let {
                    builder.append(it)
                    return@forEachIndexed // continue
                }

                val (prefix, suffix) = childAssertions[i] ?: Pair("", "")


                appendExternal(prefix)

                appendSubtree(child, builder, true, bodyIdentSize, maxDumpDepth - 1)

                appendExternal(suffix)
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