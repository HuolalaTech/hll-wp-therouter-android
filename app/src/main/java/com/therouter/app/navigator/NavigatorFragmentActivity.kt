package com.therouter.app.navigator

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.therouter.app.KotlinPathIndex.Test.FRAGMENT_HOST
import com.therouter.app.R
import com.therouter.demo.base.BaseActivity
import com.therouter.router.Autowired
import com.therouter.router.Route

@Route(path = FRAGMENT_HOST, description = "Fragment容器页")
class NavigatorFragmentActivity : BaseActivity() {

    @Autowired
    lateinit var fragment: Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_host_activity)
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commitAllowingStateLoss()
    }
}