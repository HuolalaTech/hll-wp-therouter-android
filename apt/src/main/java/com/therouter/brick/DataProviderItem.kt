package com.therouter.brick

class DataProviderItem : Comparable<DataProviderItem?> {
    var path = ""
    var className = ""
    var methodName = ""
    var returnType = ""
    var returnTypeWithParams = ""
    var priority = 0
    var fieldName = ""

    override fun compareTo(other: DataProviderItem?): Int {
        return if (other?.path == null) {
            0
        } else {
            other.path.compareTo(path)
        }
    }
}