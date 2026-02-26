package com.therouter.apt

import java.util.*
import kotlin.collections.ArrayList

class ComposeItem : Comparable<ComposeItem?> {

    var path = ""

    var className: String? = ""

    var methodName = ""

    var action = ""

    var description = ""

    var params = ArrayList<ComposeParameter>()

    override fun compareTo(routeItem: ComposeItem?): Int {
        return "${routeItem?.className}.${routeItem?.methodName}".compareTo("${this.className}.${this.methodName}")
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o !is ComposeItem) return false
        return path == o.path &&
                className == o.className &&
                methodName == o.methodName &&
                action == o.action &&
                description == o.description &&
                params == o.params
    }

    override fun hashCode(): Int {
        return Objects.hash(path, className, methodName, action, description, params)
    }
}

class ComposeParameter {
    var parameterName = ""
    var parameterClassName = ""
    var parameterSimpleClassName = ""
    var hasDefault = false
    var fieldName = ""

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o !is ComposeParameter) return false
        return parameterName == o.parameterName &&
                parameterClassName == o.parameterClassName &&
                hasDefault == o.hasDefault
    }

    override fun hashCode(): Int {
        return Objects.hash(parameterName, parameterClassName, hasDefault)
    }
}