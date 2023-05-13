package com.therouter.apt

class ActionInterceptorItem : Comparable<ActionInterceptorItem> {
    var className: String = ""
    var actionName: String = ""

    override fun compareTo(other: ActionInterceptorItem): Int {
        return toString().compareTo(other.toString())
    }

    override fun toString(): String {
        return "ActionInterceptorItem(className='$className', actionName='$actionName')"
    }
}