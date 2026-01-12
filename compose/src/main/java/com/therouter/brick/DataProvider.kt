package com.therouter.brick

import com.therouter.router.Navigator

class DataProvider<T> : Comparable<DataProvider<T>?>, Comparator<DataProvider<T>?> {
    var path = ""
    var fieldName = ""
    var priority = 0

    var returnType: Class<*> = DataProvider::class.java

    var make: ((Navigator) -> T)? = null

    override fun compareTo(other: DataProvider<T>?): Int {
        return if (other?.path == null) {
            0
        } else {
            this.priority - other.priority
        }
    }

    override fun compare(thiz: DataProvider<T>?, other: DataProvider<T>?): Int {
        return if (other == null || thiz == null) {
            0
        } else {
            thiz.priority - other.priority
        }
    }
}