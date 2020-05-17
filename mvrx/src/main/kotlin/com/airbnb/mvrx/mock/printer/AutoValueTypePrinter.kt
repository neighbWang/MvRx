package com.airbnb.mvrx.mock.printer


/**
 * This looks for classes generated via AutoValue, indicated by the class prefix [GENERATED_PREFIX].
 * When an autovalue class is found, code to reconstruct it is generated by reflectively
 * accessing the Builder and its setter functions.
 *
 * This relies on the class using the naming pattern of builder() and build() for its functions,
 * as well as Builder for its builder class.
 */
class AutoValueTypePrinter : TypePrinter<Any> {

    override fun acceptsObject(obj: Any): Boolean {
        return obj::class.java.simpleName.startsWith(GENERATED_PREFIX)
    }

    override fun generateCode(instance: Any, generateConstructor: (Any?) -> String): String {
        val kClass = instance::class
        val className = kClass.java.simpleName
        val name = className.substringAfter(GENERATED_PREFIX)

        // The builder annotation is not kept at runtime, so to find the builder class we
        // look for the generated "Builder" nested class.
        val builderClass = kClass.nestedClasses
            .filter { !it.isAbstract }
            .firstOrNull { it.simpleName?.contains("builder", ignoreCase = true) == true }
            ?.java
        // Instead of crashing we have the error msg in the generated code. This allows the
        // rest of the code to be generated while still making it clear what went wrong.
            ?: return "Error: Could not find AutoValue Builder for $className"


        val builderMethods = builderClass.declaredMethods
            // We expect all builder methods that set a property to return
            // the builder type. This helps exclude the "build" function.
            .filter { it.returnType.isAssignableFrom(builderClass) }
            .mapNotNull { builderMethod ->
                val valueConstructor =
                    kClass.java.declaredMethods // Methods may be public, protected, or package private
                        .firstOrNull { getter ->
                            getter.name == builderMethod.name ||
                                    getter.name.substringAfter("get").decapitalize() == builderMethod.name.substringAfter(
                                "set"
                            ).decapitalize()
                        }
                        ?.also { it.isAccessible = true }
                        ?.invoke(instance)
                        ?.let { generateConstructor(it) }
                        ?: return@mapNotNull null

                "\n.${builderMethod.name}($valueConstructor)"
            }
            .sorted() // Sorting gives consistent output, otherwise tests are flaky
            .joinToString(separator = "")

        // This expects the builder/build functions to have these standard names.
        return "$name.builder()$builderMethods\n.build()"
    }

    override fun modifyImports(imports: List<String>): List<String> {
        // At runtime we use the generated autovalue subclass, but our import should actually
        // reference the base class, since that is where the builder is accessed from.
        return imports.map { it.replace(GENERATED_PREFIX, "") }
    }

    companion object {
        private const val GENERATED_PREFIX = "AutoValue_"
    }
}