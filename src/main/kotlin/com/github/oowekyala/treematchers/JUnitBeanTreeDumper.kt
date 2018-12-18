package com.github.oowekyala.treematchers

/**
 * @author Clément Fournier
 * @since 1.0
 */
open class JUnitBeanTreeDumper<H : Any>(adapter: TreeLikeAdapter<H>) : BaseBeanTreeDumper<H>(adapter) {

    override fun formatPropertyAssertion(expected: Any?, actualPropertyAccess: String): String? =
            when (expected) {
                true  -> "assertTrue($actualPropertyAccess)"
                false -> "assertFalse($actualPropertyAccess)"
                null  -> "assertNull($actualPropertyAccess)"
                else  -> valueToString(expected)?.let { "assertEquals($it, $actualPropertyAccess)" }
            }

    protected fun getChildName(node: H, childIndex: Int) = "child$childIndex"

    override fun getContextAroundChildAssertion(node: H, childIndex: Int, actualPropertyAccess: String): Pair<String, String> {
        val name = getChildName(node, childIndex)
        return Pair("val $name = ",
                "\nassertEquals($name, $actualPropertyAccess)")
    }
}