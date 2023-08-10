package com.therouter.apt

class FlowTaskItem : Comparable<FlowTaskItem> {
    var async = false
    var className: String = ""
    var methodName: String = ""
    var taskName: String = ""
    var dependencies: String = ""

    override fun toString(): String {
        return "FlowTaskItem(async=$async, className='$className', methodName='$methodName', taskName='$taskName', dependencies='$dependencies')"
    }

    override fun compareTo(other: FlowTaskItem): Int {
        return toString().compareTo(other.toString())
    }
}