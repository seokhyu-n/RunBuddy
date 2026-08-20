package com.example.runbuddy.data.record

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object RecordRepository {

    private const val PREF = "records_pref"
    private const val KEY = "records_list"

    fun saveRecord(context: Context, record: Record) {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val list = getAllRecords(context).toMutableList()

        list.add(record)

        val json = Gson().toJson(list)
        prefs.edit().putString(KEY, json).apply()
    }

    fun getAllRecords(context: Context): List<Record> {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY, null) ?: return emptyList()

        val type = object : TypeToken<List<Record>>() {}.type
        return Gson().fromJson(json, type)
    }
}
