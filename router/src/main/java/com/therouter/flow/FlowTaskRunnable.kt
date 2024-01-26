package com.therouter.flow

/**
 * FlowTask执行类，仅APT生成类会使用，用于记录调用日志
 */
interface FlowTaskRunnable : Runnable {
    fun log(): String
}