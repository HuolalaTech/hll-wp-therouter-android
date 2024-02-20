package com.therouter.apt

class AOPItem : Comparable<AOPItem> {
    var point: Int = 0
    var className: String = ""
    var methodName: String = ""

    override fun compareTo(other: AOPItem): Int {
        return toString().compareTo(other.toString())
    }

    override fun toString(): String {
        return "AOPItem(point=$point, className='$className', methodName='$methodName')"
    }
}