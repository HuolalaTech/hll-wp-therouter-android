package com.therouter.compose

import android.R
import android.app.Activity
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

object ComposeViewFactory {

    public fun makeView(
        activity: Activity,
        width: Int = activity.window.decorView.findViewById<ViewGroup>(R.id.content).getChildAt(0).width,
        height: Int = activity.window.decorView.findViewById<ViewGroup>(R.id.content).getChildAt(0).height,
        parent: CompositionContext? = null,
        content: @Composable () -> Unit,
    ): ViewGroup {
        val composeView = ComposeView(activity).apply {
            setParentCompositionContext(parent)
            setContent(content)
            setOwners(activity)
            layoutParams = ViewGroup.LayoutParams(width, height)
        }
        return composeView
    }

    /**
     * These owners are not set before AppCompat 1.3+ due to a bug, so we need to set them manually in
     * case developers are using an older version of AppCompat.
     */
    private fun setOwners(activity: Activity) {
        val decorView = activity.window.decorView
        if (decorView.findViewTreeLifecycleOwner() == null && activity is LifecycleOwner) {
            decorView.setViewTreeLifecycleOwner(activity)
        }
        if (decorView.findViewTreeViewModelStoreOwner() == null && activity is ViewModelStoreOwner) {
            decorView.setViewTreeViewModelStoreOwner(activity)
        }
        if (decorView.findViewTreeSavedStateRegistryOwner() == null && activity is SavedStateRegistryOwner) {
            decorView.setViewTreeSavedStateRegistryOwner(activity)
        }
    }
}