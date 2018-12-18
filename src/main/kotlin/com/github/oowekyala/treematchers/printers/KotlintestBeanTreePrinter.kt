package com.github.oowekyala.treematchers.printers

import com.github.oowekyala.treematchers.TreeLikeAdapter

/**
 * A [BeanTreePrinter] that formats assertions with a kotlintest "should" syntax.
 *
 * @author Clément Fournier
 * @since 1.0
 */
open class KotlintestBeanTreePrinter<H : Any>(adapter: TreeLikeAdapter<H>) : BeanTreePrinter<H>(adapter) {

    override fun formatPropertyAssertion(expected: Any?, actualPropertyAccess: String): String? =
            valueToString(expected)?.let { "$actualPropertyAccess shouldBe $it" }

    override fun getContextAroundChildAssertion(node: H, childIndex: Int, actualPropertyAccess: String): Pair<String, String> {
        return Pair("$actualPropertyAccess shouldBe ", "")
    }
}