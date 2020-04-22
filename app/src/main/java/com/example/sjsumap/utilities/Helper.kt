package com.example.sjsumap.utilities

import android.app.Activity
import android.content.Context
import android.view.inputmethod.InputMethodManager


class Helper {
    companion object {
        fun getTextFromResources(context: Context, resourceId: Int): String {
            return context.resources.openRawResource(resourceId).use { inputStream ->
                inputStream.bufferedReader().use {
                    it.readText()
                }
            }
        }

        fun hideSoftKeyboard(activity: Activity) {
            if (activity.currentFocus == null) {
                return
            }
            val inputMethodManager: InputMethodManager =
                activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(
                activity.currentFocus!!.windowToken,
                0
            )
        }
    }
}