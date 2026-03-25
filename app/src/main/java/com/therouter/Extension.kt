package com.therouter

fun require(pass: Boolean, tag: String, msg: String) {
    if (!pass) {
        throw IllegalArgumentException("TheRouter::$tag::$msg")
    }
}
