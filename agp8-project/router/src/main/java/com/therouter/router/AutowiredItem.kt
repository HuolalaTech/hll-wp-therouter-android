package com.therouter.router

class AutowiredItem(
    type: String,
    key: String,
    id: Int,
    args: String,
    className: String,
    fieldName: String,
    required: Boolean,
    description: String
) {
    var type = ""
    var key = ""
    var id: Int
    var args = ""
    var className = ""
    var fieldName = ""
    var required: Boolean
    var description = ""

    init {
        this.type = type
        this.key = key
        this.id = id
        this.args = args
        this.className = className
        this.fieldName = fieldName
        this.required = required
        this.description = description
    }
}