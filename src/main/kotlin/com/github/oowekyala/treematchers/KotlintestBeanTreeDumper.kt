package com.github.oowekyala.treematchers

/**
 * @author Clément Fournier
 * @since 1.0
 */
open class KotlintestBeanTreeDumper<H : Any>(adapter: TreeLikeAdapter<H>) : BaseBeanTreeDumper<H>(adapter) {

    override fun formatPropertyAssertion(expected: Any?, actualPropertyAccess: String): String? =
            valueToString(expected)?.let { "$actualPropertyAccess shouldBe $it" }

    override fun getContextAroundChildAssertion(node: H, childIndex: Int, actualPropertyAccess: String): Pair<String, String> {
        return Pair("$actualPropertyAccess shouldBe ", "")
    }
}