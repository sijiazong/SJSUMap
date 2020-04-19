package com.example.sjsumap.utilities

import android.content.Context

class FileHelper {
    companion object {
        fun getTextFromResources(context: Context, resourceId: Int): String {
            return context.resources.openRawResource(resourceId).use { inputStream ->
                inputStream.bufferedReader().use {
                    it.readText()
                }
            }
        }
    }
}