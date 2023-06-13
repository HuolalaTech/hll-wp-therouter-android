package com.therouter.app.navigator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.therouter.app.KotlinPathIndex.Test.FRAGMENT_TEST
import com.therouter.app.R
import com.therouter.app.router.InternalBeanTest.RowBean
import com.therouter.demo.base.BaseFragment
import com.therouter.router.Autowired
import com.therouter.router.Route

@Route(path = FRAGMENT_TEST, description = "Fragment测试页")
open class NavigatorFragment : BaseFragment() {

    //    .withInt("intValue", 12345678) // 测试传 int 值
    //    .withString("stringIntValue", "12345678")// 测试用 string 传 int 值
    //    .withString("str_123_Value", "测试传中文字符串")// 测试 string
    //    .withString("boolParseError", "非boolean值") // 测试用 boolean 解析字符串的情况
    //    .withString("shortParseError", "12345678") // 测试用 short 解析超长数字的情况
    //    .withBoolean("boolValue", true) // 测试 boolean
    //    .withLong("longValue", 123456789012345L)  // 测试 long
    //    .withChar("charValue", 'c')  // 测试 char
    //    .withDouble("double", 3.14159265358972)// 测试double，key与关键字冲突
    // 测试int值传递
    @JvmField
    @Autowired
    var intValue = 0

    @JvmField
    @Autowired
    var stringIntValue: String? = null

    @JvmField
    @Autowired
    var str_123_Value: String? = null

    @JvmField
    @Autowired
    var boolParseError = false

    @JvmField
    @Autowired
    var shortParseError: Short = 0

    @JvmField
    @Autowired
    var boolValue = false

    @JvmField
    @Autowired
    var longValue: Long? = null

    @JvmField
    @Autowired
    var charValue = 0.toChar()

    @JvmField
    @Autowired(name = "double")
    var doubleValue = 0.0

    @JvmField
    @Autowired
    var floatValue = 0f

    // kotlin 代码使用 @Autowired，要么加上 @JvmField，要么使用 lateinit 修饰
    // 来自注解设置的默认值，允许路由动态修改
    @Autowired
    lateinit var strFromAnnotation: String

    @JvmField
    @Autowired(name = "SerializableObject")
    var serializableBean: RowBean? = null

    @JvmField
    @Autowired(name = "ParcelableObject")
    var parcelableBean: RowBean? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return View.inflate(getActivity(), R.layout.navigator_fragment, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textView1: TextView = view.findViewById<TextView>(R.id.textview1)
        textView1.text = "接收int值传递：integer2=$intValue"

        val textView2: TextView = view.findViewById<TextView>(R.id.textview2)
        textView2.text = "用String传递int数据：$stringIntValue"

        val textView3: TextView = view.findViewById<TextView>(R.id.textview3)
        textView3.text = "接收包含大小写数字的String值传递：$str_123_Value"

        val textView4: TextView = view.findViewById<TextView>(R.id.textview4)
        textView4.text = "接收故意传递非boolean值给boolean变量：$boolParseError"

        val textview4_1: TextView = view.findViewById<TextView>(R.id.textview4_1)
        textview4_1.text = "用字符串传一个很大的值给short变量：$shortParseError"

        val textView5: TextView = view.findViewById<TextView>(R.id.textview5)
        textView5.text = "接收boolean值：boolValue=$boolValue"

        val textview6: TextView = view.findViewById<TextView>(R.id.textview6)
        textview6.text = "接收Long类型的值：longValue=$longValue"

        val textview7: TextView = view.findViewById<TextView>(R.id.textview7)
        textview7.text = "接收Char类型的值：$charValue"

        val textview8: TextView = view.findViewById<TextView>(R.id.textview8)
        textview8.text = "接收double类型的值(key与关键字同名情况)：$doubleValue"

        val textview9: TextView = view.findViewById<TextView>(R.id.textview9)
        textview9.text = "接收float类型的值：$floatValue"

        val textview10: TextView = view.findViewById<TextView>(R.id.textview10)
        textview10.text = "接收 SerializableObject 的值：" + serializableBean!!.hello

        val textview11: TextView = view.findViewById<TextView>(R.id.textview11)
        textview11.text = "接收 ParcelableObject 的值：" + parcelableBean!!.hello
    }
}