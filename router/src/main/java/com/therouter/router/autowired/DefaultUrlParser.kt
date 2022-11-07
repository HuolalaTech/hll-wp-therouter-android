package com.therouter.router.autowired

import android.app.Activity
import android.app.Fragment
import com.therouter.router.AutowiredItem
import com.therouter.router.interceptor.AutowiredParser

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
        return null
    }
}

private fun javaTypeEqualsKotlinType(type1: String, type2: String) = when (transformNumber(type1)) {
    "java.lang.Short" -> type2 == "kotlin.Short"
    "java.lang.Byte" -> type2 == "kotlin.Byte"
    "java.lang.Integer" -> type2 == "kotlin.Int"
    "java.lang.Long" -> type2 == "kotlin.Long"
    "java.lang.Float" -> type2 == "kotlin.Float"
    "java.lang.Double" -> type2 == "kotlin.Double"
    "java.lang.Boolean" -> type2 == "kotlin.Boolean"
    "java.lang.Character" -> type2 == "kotlin.Char"
    "java.lang.String" -> type2 == "kotlin.String"
    else -> type1 == type2
}

private fun transformNumber(type: String): String {
    return when (type) {
        "byte" -> "java.lang.Byte"
        "short" -> "java.lang.Short"
        "int" -> "java.lang.Integer"
        "long" -> "java.lang.Long"
        "float" -> "java.lang.Float"
        "double" -> "java.lang.Double"
        "boolean" -> "java.lang.Boolean"
        "char" -> "java.lang.Character"
        else -> type
    }
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