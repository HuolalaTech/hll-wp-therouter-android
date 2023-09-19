package com.therouter.app.router

import android.annotation.SuppressLint
import com.therouter.app.HomePathIndex
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import com.therouter.app.R
import com.therouter.history.*
import com.therouter.router.Route
import java.lang.StringBuilder

@SuppressLint("test")
@Route(path = HomePathIndex.DEMO_HISTORY)
class HistoryLogActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "路由操作日志展示"
        setContentView(R.layout.history)
        val textView = findViewById<TextView>(R.id.content_title)

        val checkBox1 = findViewById<CheckBox>(R.id.checkbox1)
        val checkBox2 = findViewById<CheckBox>(R.id.checkbox2)
        val checkBox3 = findViewById<CheckBox>(R.id.checkbox3)
        val checkBox4 = findViewById<CheckBox>(R.id.checkbox4)
        val checkBox5 = findViewById<CheckBox>(R.id.checkbox5)

        findViewById<View>(R.id.button1).setOnClickListener {
            val level = if (checkBox1.isChecked) {
                Level.FRAGMENT
            } else {
                Level.NONE
            } + if (checkBox2.isChecked) {
                Level.ACTIVITY
            } else {
                Level.NONE
            } + if (checkBox3.isChecked) {
                Level.ACTION
            } else {
                Level.NONE
            } + if (checkBox4.isChecked) {
                Level.SERVICE_PROVIDER
            } else {
                Level.NONE
            } + if (checkBox5.isChecked) {
                Level.FLOW_TASK
            } else {
                Level.NONE
            }

            val info = StringBuilder()
            textView.text = null

            export(level).forEach {
                info.append(it).append('\n').append('\n')
            }
            textView.text = info
        }
    }
}