package com.therouter.app.navigator

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.therouter.app.HomePathIndex
import com.therouter.app.R
import com.therouter.router.Route

@Route(path = HomePathIndex.KOTLIN)
@Route(path = HomePathIndex.KOTLIN2)
class KotlinTestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.navigator_kotlin_test)
        val hello: String? = null

        val button = findViewById<Button>(R.id.button1)
        button.setOnClickListener {
            hello ?: let {
                button.text = "test"
            }
        }
    }
}