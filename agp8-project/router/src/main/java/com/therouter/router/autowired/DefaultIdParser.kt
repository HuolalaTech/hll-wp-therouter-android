package com.therouter.router.autowired

import android.app.Activity
import android.app.Fragment
import android.view.View
import com.therouter.router.AutowiredItem
import com.therouter.router.interceptor.AutowiredParser

class DefaultIdParser : AutowiredParser {
    override fun <T> parse(type: String?, target: Any?, item: AutowiredItem?): T? {
        item?.let {
            if (item.id != 0) {
                when (target) {
                    is View -> {
                        return target.findViewById<View>(item.id) as T?
                    }
                    is Activity -> {
                        return target.findViewById<View>(item.id) as T?
                    }
                    is Fragment -> {
                        return target.view!!.findViewById<View>(item.id) as T?
                    }
                    is androidx.fragment.app.Fragment -> {
                        return target.view?.findViewById<View>(item.id) as T?
                    }
                }
            }
        }
        return null
    }
}