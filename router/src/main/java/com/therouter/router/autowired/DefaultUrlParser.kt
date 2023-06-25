package com.therouter.router.autowired

import android.app.Activity
import android.app.Fragment
import android.os.Parcelable
import com.therouter.router.AutowiredItem
import com.therouter.router.interceptor.AutowiredParser
import java.io.Serializable

class DefaultUrlParser : AutowiredParser {

    override fun <T> parse(type: String?, target: Any?, item: AutowiredItem?): T? {
        if (item?.id != 0) {
            return null
        }
        when (target) {
            is Activity -> {
                return parseValue(target.intent?.extras?.get(item.key), type) as T?
            }

            is Fragment -> {
                return parseValue(target.arguments?.get(item.key), type) as T?
            }

            is androidx.fragment.app.Fragment -> {
                return parseValue(target.arguments?.get(item.key), type) as T?
            }
        }
        return null
    }

    private fun <T> parseValue(value: Any?, type: String?): T? {
        if (value == null || type == null) {
            return null
        }

        if (javaTypeEqualsKotlinType(value.javaClass.name, type)) {
            return value as T?
        }

        if (value.javaClass.name == "java.lang.String" || value.javaClass.name == "kotlin.String") {
            try {
                return transform(type, value.toString()) as T?
            } catch (e: NumberFormatException) {
            }
        }

        if (value is Serializable || value is Parcelable) {
            return value as T?
        }

        if (value.javaClass.name.contains('$') &&
            (foundClass(type)?.name == value.javaClass.name
                    || foundClass(type)?.isAssignableFrom(value.javaClass) == true)
        ) {
            return value as T?
        }

        return null
    }
}

private fun javaTypeEqualsKotlinType(type1: String, type2: String): Boolean {
    return primitive2Kotlin(type1) == primitive2Kotlin(type2)
}

private fun primitive2Kotlin(type: String) = when (type) {
    "byte" -> "kotlin.Byte"
    "short" -> "kotlin.Short"
    "int" -> "kotlin.Int"
    "long" -> "kotlin.Long"
    "float" -> "kotlin.Float"
    "double" -> "kotlin.Double"
    "boolean" -> "kotlin.Boolean"
    "char" -> "kotlin.Char"
    "java.lang.Byte" -> "kotlin.Byte"
    "java.lang.Short" -> "kotlin.Short"
    "java.lang.Integer" -> "kotlin.Int"
    "java.lang.Long" -> "kotlin.Long"
    "java.lang.Float" -> "kotlin.Float"
    "java.lang.Double" -> "kotlin.Double"
    "java.lang.Boolean" -> "kotlin.Boolean"
    "java.lang.Character" -> "kotlin.Char"
    "java.lang.String" -> "kotlin.String"
    else -> type
}

private fun transform(type: String, value: String) = when (type) {
    "byte" -> value.toByte()
    "java.lang.Byte" -> value.toByte()
    "kotlin.Byte" -> value.toByte()
    "short" -> value.toShort()
    "java.lang.Short" -> value.toShort()
    "kotlin.Short" -> value.toShort()
    "int" -> value.toInt()
    "java.lang.Integer" -> value.toInt()
    "kotlin.Int" -> value.toInt()
    "long" -> value.toLong()
    "java.lang.Long" -> value.toLong()
    "kotlin.Long" -> value.toLong()
    "float" -> value.toFloat()
    "java.lang.Float" -> value.toFloat()
    "kotlin.Float" -> value.toFloat()
    "double" -> value.toDouble()
    "java.lang.Double" -> value.toDouble()
    "kotlin.Double" -> value.toDouble()
    "boolean" -> value.toBoolean()
    "java.lang.Boolean" -> value.toBoolean()
    "kotlin.Boolean" -> value.toBoolean()
    "char" -> if (value.isNotEmpty()) {
        value[0]
    } else {
        null
    }

    "java.lang.Character" -> if (value.isNotEmpty()) {
        value[0]
    } else {
        null
    }

    "kotlin.Char" -> if (value.isNotEmpty()) {
        value[0]
    } else {
        null
    }

    else -> null
}

private fun foundClass(type: String): Class<*>? {
    if (type.contains('.')) {
        return try {
            Class.forName(type)
        } catch (e: ClassNotFoundException) {
            val index = type.lastIndexOf('.')
            foundClass(StringBuilder(type).replace(index, index + 1, "$").toString())
        }
    }
    return null
}