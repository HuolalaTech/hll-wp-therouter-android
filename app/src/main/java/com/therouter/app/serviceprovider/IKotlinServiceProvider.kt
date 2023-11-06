package com.therouter.app.serviceprovider

import android.content.Context
import android.widget.Toast
import com.therouter.demo.di.IKotlinSerivce
import com.therouter.inject.ServiceProvider

@ServiceProvider(returnType = IKotlinSerivce::class, params = [Context::class, String::class])
fun create(context: Context, str: String): IKotlinSerivce = object : IKotlinSerivce {
    override fun hello() {
        Toast.makeText(context, str, Toast.LENGTH_SHORT).show()
    }
}