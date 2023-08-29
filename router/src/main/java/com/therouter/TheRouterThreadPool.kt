@file:JvmName("TheRouterThreadPool")

package com.therouter

import android.os.Handler
import android.os.Looper
import android.util.SparseArray
import java.lang.StringBuilder
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.math.max
import kotlin.math.min

private val CPU_COUNT = Runtime.getRuntime().availableProcessors()

private val CORE_POOL_SIZE = max(3, min(CPU_COUNT - 1, 6))
private val BIGGER_CORE_POOL_SIZE = CPU_COUNT * 4
private val MAXIMUM_CORE_POOL_SIZE = CPU_COUNT * 8
private const val MAXIMUM_POOL_SIZE = Int.MAX_VALUE
private const val KEEP_ALIVE_SECONDS = 30L
private const val KEEP_ALIVE_MILLISECOND = KEEP_ALIVE_SECONDS * 1000
private const val MAX_QUEUE_SIZE = 10
private const val MAX_REPEAT_TASK_COUNT = 5
private const val CHECK_REPEAT_TASK_TIME_MILLISECOND = MAX_REPEAT_TASK_COUNT * 1000L

private const val THREAD_NAME = "TheRouterLibThread"

var executor: ExecutorService = BufferExecutor()
private val main = Handler(Looper.getMainLooper())

fun setThreadPoolExecutor(e: ExecutorService?) = e?.let {
    executor = it
}

/**
 * Executes the given command at some time in the future.  The command
 * may execute in a new thread, in a pooled thread, or in the calling
 * thread, at the discretion of the `Executor` implementation.
 *
 * @param command the runnable task
 */
fun execute(command: Runnable) {
    try {
        executor.execute(command)
    } catch (e: Exception) {
        //RejectedExecutionException if this task cannot be accepted for execution
        debug("TheRouterThreadPool", "rejected execute runnable") {
            e.printStackTrace()
        }
    }
}

fun executeInMainThread(command: Runnable): Boolean =
    if (Thread.currentThread() == Looper.getMainLooper().thread) {
        command.run()
        true
    } else {
        main.post(command)
    }

private var threadPoolExecutor = ThreadPoolExecutor(
    CORE_POOL_SIZE,
    MAXIMUM_POOL_SIZE, KEEP_ALIVE_SECONDS,
    TimeUnit.SECONDS, LinkedBlockingDeque(MAX_QUEUE_SIZE),
    newThreadFactory(THREAD_NAME)
).apply {
    allowCoreThreadTimeOut(true)
}

fun newThreadFactory(threadName: String): ThreadFactory {
    return object : ThreadFactory {
        private val mCount = AtomicInteger(1)
        override fun newThread(r: Runnable): Thread {
            return Thread(r, threadName + " #" + mCount.getAndIncrement())
        }
    }
}

/**
 * 二级缓存线程池内部执行逻辑
 *
 * <pre>
 * 每个任务首先被加入二级缓存队列中
 * if (一级队列没有满 && 当前正在执行的线程不超过核心线程数) {
 * 取二级队列中的任务
 * if (二级队列中的任务数超过最大队列数的100倍) {
 * 将线程池的核心线程数设置为 ${MAXIMUM_CORE_POOL_SIZE}
 * } else if (二级队列中的任务数超过最大队列数的10倍) {
 * 将线程池的核心线程数设置为 ${BIGGER_CORE_POOL_SIZE}
 * } else {
 * 将线程池的核心线程数设置为 ${CORE_POOL_SIZE}
 * }
 *
 * if (核心线程全部处于工作中) {
 * if (一级队列没有满){
 * 将任务加入一级队列
 * } else {
 * 新开临时线程执行任务
 * }
 * } else {
 * 将任务交给核心线程执行
 * }
 * }
</pre> *
 */
private class BufferExecutor : ExecutorService, Executor {
    val taskQueue = ArrayDeque<Task>()
    var activeTask: Task? = null
    val flightTaskMap = SparseArray<FlightTaskInfo>()
    var prevCheckAliveTime = 0L
    val taskTraceCountMap = HashMap<String, Int>()
    var prevCheckRepeatTime = 0L

    @Synchronized
    override fun execute(r: Runnable) {
        val trace = getTrace(Thread.currentThread().stackTrace)
        checkTask(trace)
        taskQueue.offer(Task(r, trace) {
            flightTaskMap.remove(r.hashCode())
            scheduleNext()
        })
        //activeTask 不为空，表示一级队列已经满了，此刻任务应该被停留到二级队列等待调度
        if (activeTask == null) {
            scheduleNext()
        }
    }

    /**
     * 检查是否有频繁添加任务，或有轮询任务的情况
     */
    private fun checkTask(trace: String) {
        if (TheRouter.isDebug) {
            if (System.currentTimeMillis() - prevCheckAliveTime > KEEP_ALIVE_MILLISECOND) {
                flightTaskMap.forEach { k, v ->
                    if (System.currentTimeMillis() - v.createTime > KEEP_ALIVE_MILLISECOND) {
                        v.resetTime()
                        debug("ThreadPool", "该任务耗时过久，请判断是否需要优化代码\n${v.trace}")
                    }
                }
                prevCheckAliveTime = System.currentTimeMillis()
            }
            var count = taskTraceCountMap[trace] ?: 0
            count++
            if (System.currentTimeMillis() - prevCheckRepeatTime > CHECK_REPEAT_TASK_TIME_MILLISECOND) {
                if (count > MAX_REPEAT_TASK_COUNT) {
                    count = 0
                    debug("ThreadPool", "该任务被频繁添加，请判断是否需要优化代码\n${trace}")
                }
            }
            taskTraceCountMap[trace] = count
        } else {
            flightTaskMap.clear()
            taskTraceCountMap.clear()
        }
    }

    /**
     * 从二级队列调度任务，将任务放入一级队列，或直接交由线程池执行
     */
    @Synchronized
    private fun scheduleNext() {
        //线程池中正在执行的任务数
        val activeCount = threadPoolExecutor.activeCount
        //一级队列任务数
        val queueSize = threadPoolExecutor.queue.size

        //动态修改核心线程数，以适应不同场景的任务量
        when {
            taskQueue.size > MAX_QUEUE_SIZE * 100 -> {
                threadPoolExecutor.corePoolSize = MAXIMUM_CORE_POOL_SIZE
            }

            taskQueue.size > MAX_QUEUE_SIZE * 10 -> {
                threadPoolExecutor.corePoolSize = BIGGER_CORE_POOL_SIZE
            }

            else -> {
                threadPoolExecutor.corePoolSize = CORE_POOL_SIZE
            }
        }

        //如果一级队列没有满，且当前正在执行的线程不超过核心线程数
        if (queueSize <= MAX_QUEUE_SIZE && activeCount < threadPoolExecutor.corePoolSize) {
            //从二级队列中取任务
            if (taskQueue.poll().also { activeTask = it } != null) {
                activeTask?.let { flightTaskMap.put(it.r.hashCode(), FlightTaskInfo(it.trace)) }
                //将任务加入一级队列，或有可能直接被线程池执行(Executor内部逻辑)
                threadPoolExecutor.execute(activeTask)
                activeTask = null
            }
        }
    }

    override fun shutdown() {
        threadPoolExecutor.shutdown()
    }

    override fun shutdownNow(): List<Runnable> {
        return threadPoolExecutor.shutdownNow()
    }

    override fun isShutdown(): Boolean {
        return threadPoolExecutor.isShutdown
    }

    override fun isTerminated(): Boolean {
        return threadPoolExecutor.isTerminated
    }

    @Throws(InterruptedException::class)
    override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean {
        return threadPoolExecutor.awaitTermination(timeout, unit)
    }

    override fun <T> submit(task: Callable<T>): Future<T> {
        return threadPoolExecutor.submit(task)
    }

    override fun <T> submit(task: Runnable, result: T): Future<T> {
        return threadPoolExecutor.submit(task, result)
    }

    override fun submit(task: Runnable): Future<*> {
        return threadPoolExecutor.submit(task)
    }

    @Throws(InterruptedException::class)
    override fun <T> invokeAll(tasks: Collection<Callable<T>?>): List<Future<T>> {
        return threadPoolExecutor.invokeAll(tasks)
    }

    @Throws(InterruptedException::class)
    override fun <T> invokeAll(tasks: Collection<Callable<T>?>, timeout: Long, unit: TimeUnit): List<Future<T>> {
        return threadPoolExecutor.invokeAll(tasks, timeout, unit)
    }

    @Throws(ExecutionException::class, InterruptedException::class)
    override fun <T> invokeAny(tasks: Collection<Callable<T>?>): T {
        return threadPoolExecutor.invokeAny(tasks)
    }

    @Throws(ExecutionException::class, InterruptedException::class, TimeoutException::class)
    override fun <T> invokeAny(tasks: Collection<Callable<T>?>, timeout: Long, unit: TimeUnit): T {
        return threadPoolExecutor.invokeAny(tasks, timeout, unit)
    }
}

private class FlightTaskInfo(val trace: String) {
    var createTime = System.currentTimeMillis()
        private set

    fun resetTime() {
        createTime = System.currentTimeMillis()
    }
}

private class Task(
    val r: Runnable,
    val trace: String,
    val block: () -> Unit
) : Runnable {
    override fun run() = try {
        r.run()
    } finally {
        block()
    }
}

private fun getTrace(trace: Array<StackTraceElement>): String {
    val str = StringBuilder()
    trace.forEach {
        str.append(it).append('\n')
    }
    return str.toString()
}

private inline fun <T> SparseArray<T>.forEach(action: (key: Int, value: T) -> Unit) {
    for (index in 0 until size()) {
        action(keyAt(index), valueAt(index))
    }
}
