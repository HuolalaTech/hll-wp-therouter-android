package com.therouter.router

class PendingNavigator(val navigator: Navigator, val action: () -> Unit) {
    override fun equals(other: Any?): Boolean {
        if (other is PendingNavigator) {
            return other.navigator == navigator
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return navigator.hashCode() + 1
    }
}
