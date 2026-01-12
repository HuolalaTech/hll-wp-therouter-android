package com.therouter.ksp

class DataProviderItem : Comparable<DataProviderItem?> {
    var type = ""
    var className = ""
    var methodName = ""
    var returnType = ""
    var priority = 0

    override fun compareTo(other: DataProviderItem?): Int {
        return if (other?.type == null) {
            0
        } else {
            other.type.compareTo(type)
        }
    }
}