package com.github.oowekyala.treeutils.printers

import com.github.oowekyala.treeutils.TreeLikeAdapter
import java.beans.BeanInfo
import java.beans.IntrospectionException
import java.beans.Introspector
import java.beans.PropertyDescriptor
import java.lang.reflect.InvocationTargetException
import java.util.*

/**
 * A [DslStructurePrinter] that adds assertions for the properties accessible as beans.
 * Useful to e.g. generate unit tests.
 *
 * Prints only properties declared on the node's runtime class by default.
 * * override [takePropertyDescriptorIf] to specify a different filter
 * * override [valueToString] to support more property types
 *
 * @author Clément Fournier
 * @since 1.0
 */
abstract class BeanTreePrinter<H : Any>(adapter: TreeLikeAdapter<H>) : DslStructurePrinter<H>(adapter) {

    /**
     * Returns true if the property [prop] of [node] should be dumped as an assertion.
     * Property descriptors are previously filtered to those that *can* be dumped,
     * by [valueToString].
     *
     * By default, filters out properties that were not declared on the node's runtime class.
     */
    protected open fun takePropertyDescriptorIf(node: H, prop: PropertyDescriptor): Boolean =
            prop.readMethod?.declaringClass === node.javaClass

    /**
     * Gets the bean property descriptors of the given class. Uses [java.beans.Introspector.getPropertyDescriptors]
     * by default.
     */
    protected open fun getAllPropertyDescriptors(node: H): List<PropertyDescriptor> = myGetPropertyDescriptors(node.javaClass)

    /**
     * Formats an assertion asserting the property [actualPropertyAccess] is
     * the object [expected]. This can be tuned to your preferred testing syntax,
     * e.g. see [JUnitBeanTreePrinter] and [KotlintestBeanTreePrinter].
     *
     * Example input: if a node's class has a method `getName()`, which returns
     * the string `"foo"`, then this method will be called with parameters
     * `(expected = "foo", actualPropertyAccess = "it.name"`). You may e.g. return
     * `"$actualPropertyAccess shouldBe ${valueToString(expected)}"`, or switch
     * on the type of the value to select another assertion method.
     *
     * @return A formatted assertion string. If null then the assertion won't be rendered
     */
    protected abstract fun formatPropertyAssertion(expected: Any?, actualPropertyAccess: String): String?

    /**
     * Formats an assertion asserting the property [actualPropertyAccess] is
     * equal to the given [node], which is the [childIndex]th child of his
     * parent.
     *
     * @return a pair of (prefix, suffix) which will surround the `child<..>{..}` call corresponding to [node]
     */
    protected abstract fun getContextAroundChildAssertion(node: H, childIndex: Int, actualPropertyAccess: String): Pair<String, String>

    private fun getKotlinPropertyName(prop: PropertyDescriptor) =
    // take care of Kotlin property access conventions
            when (prop.readMethod.returnType) {
                Boolean::class.java, java.lang.Boolean::class.java ->
                    if (prop.readMethod.name.startsWith("is")) prop.readMethod.name
                    else prop.name
                else                                               -> prop.name
            }

    private fun getPropertyToValue(node: H): Map<PropertyDescriptor, Any?> =
            getAllPropertyDescriptors(node)
                    .filter { it.readMethod != null }
                    .mapNotNull { prop ->
                        val value = try {
                            prop.readMethod.invoke(node)
                        } catch (e: Exception) {
                            when (e) {
                                is IllegalAccessException, is InvocationTargetException -> return@mapNotNull null
                                else                                                    -> throw e
                            }
                        }

                        Pair(prop, value)
                    }.toMap()

    final override fun getAdditionalAssertions(node: H): List<String> =
            getPropertyToValue(node)
                    .asSequence()
                    .filter { (_, value) -> valueToString(value) != null }
                    .filter { (prop, _) -> takePropertyDescriptorIf(node, prop) }
                    .mapNotNull { (prop, v) ->
                        formatPropertyAssertion(expected = v, actualPropertyAccess = "it.${getKotlinPropertyName(prop)}")
                    }
                    .toList()

    final override fun getChildCallContexts(parent: H): Map<Int, Pair<String, String>> =
            getPropertyToValue(parent)
                    .asSequence()
                    .filter { (prop, _) -> takePropertyDescriptorIf(parent, prop) }
                    .mapNotNull { (prop, value) ->

                        val idx = adapter.getChildren(parent).indexOfFirst { it === value }

                        @Suppress("UNCHECKED_CAST")
                        if (idx < 0) null
                        else Pair(
                                idx,
                                getContextAroundChildAssertion(value as H,
                                        idx,
                                        "it." + getKotlinPropertyName(prop))
                        )
                    }.toMap()

    /**
     * Returns a string that can represent the [value] in
     * a Kotlin source file. If this method returns null,
     * no assertion will be generated for the property the
     * value comes from.
     *
     * The default supports primitive types, [Class],
     * enum constants, [String] and null values.
     */
    protected open fun valueToString(value: Any?): String? {
        return when (value) {
            is String                   ->
                value.replace("\"".toRegex(), "\\\"")
                        // escape kt string interpolators
                        .replace("\\$(?=[a-zA-Z{])".toRegex(), "\\\${'\\$'}")
                        .let { "\"$it\"" }
            is Char                     -> '\''.toString() + value.toString().replace("'".toRegex(), "\\'") + '\''.toString()
            is Enum<*>                  -> value.enumDeclaringClass.canonicalName + "." + value.name
            is Class<*>                 -> value.canonicalName?.let { "$it::class.java" }
            is Number, is Boolean, null -> value.toString()
            else                        -> null
        }
    }

    private companion object {

        private val Enum<*>.enumDeclaringClass: Class<*>
            get() = this.javaClass.let {
                when {
                    it.isEnum -> it
                    else      -> it.enclosingClass.takeIf { it.isEnum }
                                 ?: throw  IllegalStateException()
                }
            }

        private val descriptorsCache: MutableMap<Class<*>, List<PropertyDescriptor>> = WeakHashMap()

        private fun myGetPropertyDescriptors(beanClass: Class<*>): List<PropertyDescriptor> {
            return descriptorsCache.computeIfAbsent(beanClass) {
                val beanInfo: BeanInfo? = try {
                    Introspector.getBeanInfo(beanClass)
                } catch (_: IntrospectionException) {
                    null
                }

                beanInfo?.propertyDescriptors?.toList() ?: emptyList()
            }
        }
    }
}
