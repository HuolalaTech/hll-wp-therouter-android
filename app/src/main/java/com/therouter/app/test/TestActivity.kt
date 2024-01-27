package com.therouter.app.test

import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
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
            "http://therouter.cn/index.html?version=1.2.2-rc5&os=android&_t=1706323480253&tokenC400EA04D27#/statistics/index?&version=1.2.2&os=android",
            "http://therouter.cn/index.html?version=1&os=android&_t=1706323480253&tokenC400EA04D27/#abc/statistics/index&os=android",
            "http://therouter.cn/index.html?version=1&tokenC400EA04D27#/statistics/index"
        )

        array.forEach {
            try {
                val uri = Uri.parse(it)
                println("\n==============解析:${uri}")
                parserString(uri.encodedQuery)
                parserString(uri.encodedFragment)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun parserString(url: String?) {
        if (url?.contains('?') == true) {
            val index = url.indexOf('?')
            if (index > -1) {
                val item1 = url.substring(0, index)
                // ?后面的部分都作为当前url的参数直接拼接
                val appendValue = url.substring(index + 1)
                val list = item1.split('&')
                if (list.isNotEmpty()) {
                    for (i in list.indices) {
                        if (i == list.size - 1) {
                            parser(list[i], appendValue)
                        } else {
                            parser(list[i])
                        }
                    }
                }
            }
        } else {
            url?.split("&")?.forEach { str ->
                parser(str)
            }
        }
    }

    private fun parser(kvPairString: String?, appendValue: String? = "") {
        if (!kvPairString.isNullOrBlank() && kvPairString.trim() != "=") {
            val index = kvPairString.indexOf("=")
            //  http://therouter.cn/page?a&b=&=c
            //  这个url中,a和b都被认为是只有k没有v的参数,c被认为只有v没有k的参数
            var key = ""
            var value = ""
            when (index) {
                -1 -> {
                    key = kvPairString
                }

                0 -> {
                    value = kvPairString.substring(1)
                }

                else -> {
                    key = kvPairString.substring(0, index)
                    value = kvPairString.substring(index + 1)
                }
            }
            if (!TextUtils.isEmpty(appendValue?.trim())) {
                value += appendValue
            }
            println("$key=$value")
        }
    }
}