package com.therouter.app.navigator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.therouter.app.KotlinPathIndex.Test.VIEW_BINDING_FRAGMENT_TEST
import com.therouter.app.R
import com.therouter.app.databinding.ViewbindingTestBinding
import com.therouter.demo.base.BaseFragment
import com.therouter.router.Route

@Route(path = VIEW_BINDING_FRAGMENT_TEST, description = "ViewBinding测试页")
open class ViewBindingTestFragment : BaseFragment() {

    var fragmentCameraBinding: ViewbindingTestBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentCameraBinding = ViewbindingTestBinding.inflate(inflater, container, false)
        return fragmentCameraBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fragmentCameraBinding?.textview1?.text = "测试"

        val textView2: TextView = view.findViewById<TextView>(R.id.textview2)

        val textView3: TextView = view.findViewById<TextView>(R.id.textview3)

        val textView4: TextView = view.findViewById<TextView>(R.id.textview4)

        val textView5: TextView = view.findViewById<TextView>(R.id.textview5)

        val textview6: TextView = view.findViewById<TextView>(R.id.textview6)

        val textview7: TextView = view.findViewById<TextView>(R.id.textview7)

        val textview8: TextView = view.findViewById<TextView>(R.id.textview8)

        val textview9: TextView = view.findViewById<TextView>(R.id.textview9)

        val textview10: TextView = view.findViewById<TextView>(R.id.textview10)
    }
}