package com.mymusic.repository

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mymusic.app.AppContainer
import com.mymusic.rest.ApiService

class RestRepository(
    private val apiService: ApiService = AppContainer.apiService
) {
    suspend fun getRecommends(list: List<String>): List<String> {
        return getList(apiService.getRecommends(getJsonObject(list)))
    }

    private fun getJsonObject(list: List<String>): JsonObject {
        val jsonObject = JsonObject()
        val jsonArray = JsonArray()
        list.forEach { jsonArray.add(it) }
        jsonObject.add("songs", jsonArray)
        return jsonObject
    }

    private fun getList(jsonObject: JsonObject): List<String> {
        val list = mutableListOf<String>()
        val jsonArray = jsonObject.getAsJsonArray("predicted_songs")
        jsonArray.forEach {
            list.add(it.asString)
        }
        return list
    }

}