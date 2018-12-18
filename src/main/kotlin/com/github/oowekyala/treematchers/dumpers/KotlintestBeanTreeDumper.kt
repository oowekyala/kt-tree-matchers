package com.github.oowekyala.treematchers.dumpers

import com.github.oowekyala.treematchers.TreeLikeAdapter

/**
 * A [BeanTreeDumper] that formats assertions with a kotlintest "should" syntax.
 *
 * @author Clément Fournier
 * @since 1.0
 */
open class KotlintestBeanTreeDumper<H : Any>(adapter: TreeLikeAdapter<H>) : BeanTreeDumper<H>(adapter) {

    override fun formatPropertyAssertion(expected: Any?, actualPropertyAccess: String): String? =
            valueToString(expected)?.let { "$actualPropertyAccess shouldBe $it" }

    override fun getContextAroundChildAssertion(node: H, childIndex: Int, actualPropertyAccess: String): Pair<String, String> {
        return Pair("$actualPropertyAccess shouldBe ", "")
    }
}