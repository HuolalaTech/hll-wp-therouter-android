package com.therouter.app.navigator

import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.therouter.app.KotlinPathIndex.Test.FRAGMENT_TEST2
import com.therouter.app.R
import com.therouter.require
import com.therouter.router.Autowired
import com.therouter.router.Route

@Route(path = FRAGMENT_TEST2, description = "Fragment测试页")
class NavigatorFragment2 : NavigatorFragment() {

    @Autowired
    lateinit var stringChildClassField: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val textView: TextView = view.findViewById<TextView>(R.id.textview12)
        textView.text = "子类 @Autowired 数据：$stringChildClassField"
        require(
            "数据在子类解析" == stringChildClassField,
            "NavigatorFragment2",
            "stringChildClassField数值不对"
        )
    }
}