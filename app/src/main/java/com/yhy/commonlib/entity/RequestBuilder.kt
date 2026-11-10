package com.yhy.commonlib.entity

import com.google.gson.JsonObject

/**
 * desc:
 **
 * user: xujj
 * time: 2026/7/16 11:19
 **/
open class RequestBuilder() {

    private val params = mutableMapOf<String, Any>()

    operator fun set(key: String, value: Any) = apply {
        params[key] = value
    }

    fun build(): JsonObject {
        return JsonObject().apply {
            params.forEach { (key, value) ->
                when (value) {
                    is String -> addProperty(key, value)
                    is Number -> addProperty(key, value)
                    is Boolean -> addProperty(key, value)
                }
            }
        }
    }
}