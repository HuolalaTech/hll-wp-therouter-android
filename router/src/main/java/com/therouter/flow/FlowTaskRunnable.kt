package com.therouter.flow

interface FlowTaskRunnable : Runnable {
    fun log(): String
}