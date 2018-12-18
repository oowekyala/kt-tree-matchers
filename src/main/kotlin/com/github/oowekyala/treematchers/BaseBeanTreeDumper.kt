package com.github.oowekyala.treematchers

import java.beans.BeanInfo
import java.beans.IntrospectionException
import java.beans.Introspector
import java.beans.PropertyDescriptor
import java.lang.reflect.InvocationTargetException
import java.util.*

/**
 * A dumper that adds assertions for the properties accessible as beans.
 *
 * @author Clément Fournier
 * @since 1.0
 */
abstract class BaseBeanTreeDumper<H : Any>(adapter: TreeLikeAdapter<H>) : TreeDumper<H>(adapter) {

    protected open fun takePropertyDescriptorIf(node: H, prop: PropertyDescriptor): Boolean = true

    protected open fun getAllPropertyDescriptors(node: H): List<PropertyDescriptor> = myGetPropertyDescriptors(node.javaClass)

    protected abstract fun formatPropertyAssertion(expected: Any?, actualPropertyAccess: String): String?

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
                    .filter { (prop, _) -> takePropertyDescriptorIf(node, prop) }
                    .filter { (_, value) -> valueToString(value) != null }
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

    // returns null if the value is unsupported
    protected open fun valueToString(value: Any?): String? {
        return when (value) {
            is String                   ->
                value.replace("\"".toRegex(), "\\\"")
                        // escape kt string interpolators
                        .replace("\\$(?=[a-zA-Z{])".toRegex(), "\\\${'\\$'}")
                        .let { "\"$it\"" }
            is Char                     -> '\''.toString() + value.toString().replace("'".toRegex(), "\\'") + '\''.toString()
            is Enum<*>                  -> value.enumDeclaringClass.canonicalName + "." + value.name
            is Class<*>                 -> value.canonicalName + "::class.java"
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
