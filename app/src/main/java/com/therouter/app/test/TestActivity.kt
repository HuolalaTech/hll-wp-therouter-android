package com.therouter.app.test

import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import com.therouter.TheRouter
import com.therouter.app.HomePathIndex
import com.therouter.app.R
import com.therouter.router.Route

@Route(path = HomePathIndex.TEST_ONLY_NO_UI)
class TestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "非UI测试使用"
        setContentView(R.layout.test)

        testUrlParser()
    }

    private fun testUrlParser() {
        val array = arrayOf(
            "http://therouter.cn/page",
            "http://therouter.cn/page?",
            "http://therouter.cn/page?a&b=&=c",
            "http://therouter.cn/page?=&b=c=d",
            "http://therouter.cn/page?&a=b&path=/HLSignContractPage?k=v&id=0&a=b",
            "http://therouter.cn/page?&a=b&path=/HLSignContractPage?k=v&id=0&a=b&path=/HLSignContractPage?k=v&a=b",
            "http://therouter.cn/page?hello=world&path=/HLSignContractPage?k=v&id=0&a=b",
            "http://therouter.cn/page?k=v&你=我&%E4%BD%A0=%E6%88%91",
            "http://therouter.cn/page/#abc?k=v",
            "https://therouter.cn/page?code=123&u=1&tpl=2&bl=8#/",
            "https://therouter.cn/page?code=123&u=1&tpl=2&bl=8##/",
            "https://therouter.cn/page?code=123&u=1&tpl=2&bl=8%23#/",
            "http://therouter.cn/index.html?version=1.2.2-rc5&os=android&_t=1706323480253&tokenC400EA04D27#/statistics/index?v=1.2.2&os=android",
            "http://therouter.cn/index.html?version=1&os=android&_t=1706323480253&tokenC400EA04D27/#abc/statistics/index&os=android",
            "http://therouter.cn/index.html?version=1&tokenC400EA04D27#/statistics/index"
        )

        array.forEach {
            try {
                println("解析:$it")
                val url = TheRouter.build(it).withString("code", "456").getUrlWithParams()
                println("处理$url")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}