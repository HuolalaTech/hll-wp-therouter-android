package com.therouter.router.autowired

import android.app.Activity
import android.app.Fragment
import android.text.TextUtils
import com.therouter.router.AutowiredItem
import com.therouter.router.KEY_BUNDLE
import com.therouter.router.interceptor.AutowiredParser

const val DEFAULT_INT = -1008611
const val DEFAULT_DOUBLE = -1008611.1
const val DEFAULT_FLOAT = -1008611.1F
const val DEFAULT_CHAR = '`'
const val DEFAULT_SHORT: Short = -10086
const val DEFAULT_BYTE: Byte = -122
const val DEFAULT_LONG = -1008611L
const val DEFAULT_STRING = "therouter_parser_default_string"

class DefaultUrlParser : AutowiredParser {

    override fun <T> parse(type: String?, target: Any?, item: AutowiredItem?): T? {
        if (item?.id != 0) {
            return null
        }
        if ("java.lang.String".equals(type, ignoreCase = true) || "String".equals(type, ignoreCase = true)) {
            when (target) {
                is Activity -> {
                    return target.intent?.extras?.get(item.key)?.toString() as T?
                }
                is Fragment -> {
                    return target.arguments?.get(item.key)?.toString() as T?
                }
                is androidx.fragment.app.Fragment -> {
                    return target.arguments?.get(item.key)?.toString() as T?
                }
            }
        } else if ("int".equals(type, ignoreCase = true) || "Integer".equals(type, ignoreCase = true)
            || "java.lang.Integer".equals(type, ignoreCase = true)
        ) {
            when (target) {
                is Activity -> {
                    val value = target.intent?.getIntExtra(item.key, DEFAULT_INT)
                    if (value != null && value != DEFAULT_INT) {
                        return value as T
                    }
                    return toInt(target.intent?.getStringExtra(item.key), 0) as T?
                }
                is Fragment -> {
                    val value = target.arguments?.getInt(item.key, DEFAULT_INT)
                    if (value != null && value != DEFAULT_INT) {
                        return value as T
                    }
                    return toInt(target.arguments?.getString(item.key), 0) as T?
                }
                is androidx.fragment.app.Fragment -> {
                    val value = target.arguments?.getInt(item.key, DEFAULT_INT)
                    if (value != null && value != DEFAULT_INT) {
                        return value as T
                    }
                    return toInt(target.arguments?.getString(item.key), 0) as T?
                }
            }
        } else if ("double".equals(type, ignoreCase = true) || "java.lang.Double".equals(type, ignoreCase = true)) {
            when (target) {
                is Activity -> {
                    val value = target.intent?.getDoubleExtra(item.key, DEFAULT_DOUBLE)
                    if (value != null && value != DEFAULT_DOUBLE) {
                        return value as T
                    }
                    return toDouble(target.intent?.getStringExtra(item.key), 0.0) as T?
                }
                is Fragment -> {
                    val value = target.arguments?.getDouble(item.key, DEFAULT_DOUBLE)
                    if (value != null && value != DEFAULT_DOUBLE) {
                        return value as T
                    }
                    return toDouble(target.arguments?.getString(item.key), 0.0) as T?
                }
                is androidx.fragment.app.Fragment -> {
                    val value = target.arguments?.getDouble(item.key, DEFAULT_DOUBLE)
                    if (value != null && value != DEFAULT_DOUBLE) {
                        return value as T
                    }
                    return toDouble(target.arguments?.getString(item.key), 0.0) as T?
                }
            }
        } else if ("float".equals(type, ignoreCase = true) || "java.lang.Float".equals(type, ignoreCase = true)) {
            when (target) {
                is Activity -> {
                    val value = target.intent?.getFloatExtra(item.key, DEFAULT_FLOAT)
                    if (value != null && value != DEFAULT_FLOAT) {
                        return value as T
                    }
                    return toFloat(target.intent?.getStringExtra(item.key), 0f) as T?
                }
                is Fragment -> {
                    val value = target.arguments?.getFloat(item.key, DEFAULT_FLOAT)
                    if (value != null && value != DEFAULT_FLOAT) {
                        return value as T
                    }
                    return toFloat(target.arguments?.getString(item.key), 0f) as T?
                }
                is androidx.fragment.app.Fragment -> {
                    val value = target.arguments?.getFloat(item.key, DEFAULT_FLOAT)
                    if (value != null && value != DEFAULT_FLOAT) {
                        return value as T
                    }
                    return toFloat(target.arguments?.getString(item.key), 0f) as T?
                }
            }
        } else if ("char".equals(type, ignoreCase = true) || "Character".equals(type, ignoreCase = true)
            || "java.lang.Character".equals(type, ignoreCase = true)
        ) {
            when (target) {
                is Activity -> {
                    val value = target.intent?.getCharExtra(item.key, DEFAULT_CHAR)
                    if (value != null && value != DEFAULT_CHAR) {
                        return value as T
                    }
                    return toChar(target.intent?.getStringExtra(item.key), '0') as T?
                }
                is Fragment -> {
                    val value = target.arguments?.getChar(item.key, DEFAULT_CHAR)
                    if (value != null && value != DEFAULT_CHAR) {
                        return value as T
                    }
                    return toChar(target.arguments?.getString(item.key), '0') as T?
                }
                is androidx.fragment.app.Fragment -> {
                    val value = target.arguments?.getChar(item.key, DEFAULT_CHAR)
                    if (value != null && value != DEFAULT_CHAR) {
                        return value as T
                    }
                    return toChar(target.arguments?.getString(item.key), '0') as T?
                }
            }
        } else if ("short".equals(type, ignoreCase = true) || "java.lang.Short".equals(type, ignoreCase = true)) {
            when (target) {
                is Activity -> {
                    val value = target.intent?.getShortExtra(item.key, DEFAULT_SHORT)
                    if (value != null && value != DEFAULT_SHORT) {
                        return value as T
                    }
                    return toShort(target.intent?.getStringExtra(item.key), 0.toShort()) as T?
                }
                is Fragment -> {
                    val value = target.arguments?.getShort(item.key, DEFAULT_SHORT)
                    if (value != null && value != DEFAULT_SHORT) {
                        return value as T
                    }
                    return toShort(target.arguments?.getString(item.key), 0.toShort()) as T?
                }
                is androidx.fragment.app.Fragment -> {
                    val value = target.arguments?.getShort(item.key, DEFAULT_SHORT)
                    if (value != null && value != DEFAULT_SHORT) {
                        return value as T
                    }
                    return toShort(target.arguments?.getString(item.key), 0.toShort()) as T?
                }
            }
        } else if ("byte".equals(type, ignoreCase = true) || "java.lang.Byte".equals(type, ignoreCase = true)) {
            when (target) {
                is Activity -> {
                    val value = target.intent?.getByteExtra(item.key, DEFAULT_BYTE)
                    if (value != null && value != DEFAULT_BYTE) {
                        return value as T
                    }
                    return toByte(target.intent?.getStringExtra(item.key), 0.toByte()) as T?
                }
                is Fragment -> {
                    val value = target.arguments?.getByte(item.key, DEFAULT_BYTE)
                    if (value != null && value != DEFAULT_BYTE) {
                        return value as T
                    }
                    return toByte(target.arguments?.getString(item.key), 0.toByte()) as T?
                }
                is androidx.fragment.app.Fragment -> {
                    val value = target.arguments?.getByte(item.key, DEFAULT_BYTE)
                    if (value != null && value != DEFAULT_BYTE) {
                        return value as T
                    }
                    return toByte(target.arguments?.getString(item.key), 0.toByte()) as T?
                }
            }
        } else if ("long".equals(type, ignoreCase = true) || "java.lang.Long".equals(type, ignoreCase = true)) {
            when (target) {
                is Activity -> {
                    val value = target.intent?.getLongExtra(item.key, DEFAULT_LONG)
                    if (value != null && value != DEFAULT_LONG) {
                        return value as T
                    }
                    return toLong(target.intent?.getStringExtra(item.key), 0L) as T?
                }
                is Fragment -> {
                    val value = target.arguments?.getLong(item.key, DEFAULT_LONG)
                    if (value != null && value != DEFAULT_LONG) {
                        return value as T
                    }
                    return toLong(target.arguments?.getString(item.key), 0) as T?
                }
                is androidx.fragment.app.Fragment -> {
                    val value = target.arguments?.getLong(item.key, DEFAULT_LONG)
                    if (value != null && value != DEFAULT_LONG) {
                        return value as T
                    }
                    return toLong(target.arguments?.getString(item.key), 0) as T?
                }
            }
        } else if ("boolean".equals(type, ignoreCase = true) || "java.lang.Boolean".equals(type, ignoreCase = true)) {
            when (target) {
                is Activity -> {
                    val str = target.intent?.getStringExtra(item.key)
                    if (str != null) {
                        if (str.equals("true", true) || str.equals("false", true)) {
                            return toBoolean(str) as T?
                        }
                    }
                    return target.intent?.getBooleanExtra(item.key, false) as T?
                }
                is Fragment -> {
                    val str = target.arguments?.getString(item.key)
                    if (str != null) {
                        if (str.equals("true", true) || str.equals("false", true)) {
                            return toBoolean(str) as T?
                        }
                    }
                    return target.arguments?.getBoolean(item.key, false) as T?
                }
                is androidx.fragment.app.Fragment -> {
                    val str = target.arguments?.getString(item.key)
                    if (str != null) {
                        if (str.equals("true", true) || str.equals("false", true)) {
                            return toBoolean(str) as T?
                        }
                    }
                    return target.arguments?.getBoolean(item.key, false) as T?
                }
            }
        }
        return null
    }
}

fun toFloat(number: String?, defaultValue: Float): Float {
    return if (TextUtils.isEmpty(number)) {
        defaultValue
    } else try {
        number!!.toFloat()
    } catch (e: NumberFormatException) {
        defaultValue
    }
}

fun toDouble(number: String?, defaultValue: Double): Double {
    return if (TextUtils.isEmpty(number)) {
        defaultValue
    } else try {
        number!!.toDouble()
    } catch (e: NumberFormatException) {
        defaultValue
    }
}

fun toInt(number: String?, defaultValue: Int): Int {
    return if (TextUtils.isEmpty(number)) {
        defaultValue
    } else try {
        number!!.toInt()
    } catch (e: NumberFormatException) {
        defaultValue
    }
}

fun toShort(number: String?, defaultValue: Short): Short {
    return if (TextUtils.isEmpty(number)) {
        defaultValue
    } else try {
        number!!.toShort()
    } catch (e: NumberFormatException) {
        defaultValue
    }
}

fun toLong(number: String?, defaultValue: Long): Long {
    return if (TextUtils.isEmpty(number)) {
        defaultValue
    } else try {
        number!!.toLong()
    } catch (e: NumberFormatException) {
        defaultValue
    }
}

fun toByte(number: String?, defaultValue: Byte): Byte {
    return if (TextUtils.isEmpty(number)) {
        defaultValue
    } else try {
        number!!.toByte()
    } catch (e: NumberFormatException) {
        defaultValue
    }
}

fun toChar(number: String?, defaultValue: Char): Char {
    return if (TextUtils.isEmpty(number)) {
        defaultValue
    } else number!![0]
}

fun toBoolean(number: String?): Boolean {
    return java.lang.Boolean.parseBoolean(number)
}