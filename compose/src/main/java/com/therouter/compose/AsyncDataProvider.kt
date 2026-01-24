package com.therouter.compose

import androidx.compose.runtime.Composable
import com.therouter.app.brick.DataProvider
import com.therouter.router.Navigator

class AsyncDataProvider<T> : DataProvider<T>() {

    var asyncMake: @Composable ((Navigator, @Composable (Any?) -> Unit) -> Unit)? = null
}