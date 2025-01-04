package com.therouter.app.test

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.therouter.TheRouter
import com.therouter.app.HomePathIndex
import com.therouter.app.R
import com.therouter.router.Autowired
import com.therouter.router.Route

@Route(path = HomePathIndex.TEST_AUTOWIRED)
class Test2Activity : AppCompatActivity() {

    @JvmField
    @Autowired(required = true)
    var test: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "测试KSP"
        setContentView(R.layout.test_autowired)
        TheRouter.inject(this)

        val textView = findViewById<TextView>(R.id.text)
        textView?.text = test
    }
}