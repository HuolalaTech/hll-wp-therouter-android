package com.therouter.apt

class AutowiredItem : Comparable<AutowiredItem> {

    var type = ""

    var key = ""

    var id = 0

    var args = ""

    var className = ""

    var classNameAndTypeParameters = ""

    var fieldName = ""

    var required = false

    var description = ""

    override fun toString(): String {
        return "AutowiredItem(type='$type', key='$key', id=$id, args='$args', className='$className', fieldName='$fieldName', required=$required, description='$description')"
    }

    override fun compareTo(other: AutowiredItem): Int {
        return toString().compareTo(other.toString())
    }
}