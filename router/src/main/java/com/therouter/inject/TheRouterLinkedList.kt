package com.therouter.inject

import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * Created by ZhangTao on 17/9/22.
 */
internal class TheRouterLinkedList<E>(override val size: Int = 16) : ReentrantReadWriteLock(), MutableSet<E> {
    private val mLinkedList: MutableSet<E>

    override fun isEmpty(): Boolean {
        return mLinkedList.isEmpty()
    }

    override operator fun contains(element: E): Boolean {
        return mLinkedList.contains(element)
    }

    override fun iterator(): MutableIterator<E> {
        return mLinkedList.iterator()
    }

    override fun add(e: E): Boolean {
        return !contains(e) && mLinkedList.add(e)
    }

    override fun remove(element: E): Boolean {
        return mLinkedList.remove(element)
    }

    override fun containsAll(elements: Collection<E>): Boolean {
        return mLinkedList.containsAll(elements)
    }

    override fun addAll(collection: Collection<E>): Boolean {
        return mLinkedList.addAll(collection)
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        return mLinkedList.removeAll(elements)
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        return mLinkedList.retainAll(elements)
    }

    override fun clear() {
        mLinkedList.clear()
    }

    init {
        mLinkedList = HashSet()
    }
}