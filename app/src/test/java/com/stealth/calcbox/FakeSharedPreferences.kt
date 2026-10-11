package com.stealth.calcbox

import android.content.SharedPreferences

/**
 * In-memory implementation of [SharedPreferences] for JVM unit testing
 * without requiring Robolectric or Android Keystore dependencies.
 */
class FakeSharedPreferences : SharedPreferences {

    private val data = mutableMapOf<String, Any?>()
    private val listeners = mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    override fun getAll(): MutableMap<String, *> = HashMap(data)

    override fun getString(key: String?, defValue: String?): String? =
        data[key] as? String ?: defValue

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        (data[key] as? Set<String>)?.toMutableSet() ?: defValues

    override fun getInt(key: String?, defValue: Int): Int =
        data[key] as? Int ?: defValue

    override fun getLong(key: String?, defValue: Long): Long =
        data[key] as? Long ?: defValue

    override fun getFloat(key: String?, defValue: Float): Float =
        data[key] as? Float ?: defValue

    override fun getBoolean(key: String?, defValue: Boolean): Boolean =
        data[key] as? Boolean ?: defValue

    override fun contains(key: String?): Boolean = data.containsKey(key)

    override fun edit(): SharedPreferences.Editor = FakeEditor()

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        listener?.let { listeners.add(it) }
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        listener?.let { listeners.remove(it) }
    }

    private inner class FakeEditor : SharedPreferences.Editor {
        private val temp = mutableMapOf<String, Any?>()
        private val removed = mutableSetOf<String>()
        private var clearAll = false

        private fun putInternal(key: String?, value: Any?): SharedPreferences.Editor {
            if (key != null) {
                removed.remove(key)
                temp[key] = value
            }
            return this
        }

        override fun putString(key: String?, value: String?): SharedPreferences.Editor = putInternal(key, value)

        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = putInternal(key, values)

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor = putInternal(key, value)

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor = putInternal(key, value)

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = putInternal(key, value)

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = putInternal(key, value)

        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) {
                temp.remove(key)
                removed.add(key)
            }
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            clearAll = true
            temp.clear()
            removed.clear()
            return this
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (clearAll) {
                data.clear()
            }
            removed.forEach { data.remove(it) }
            temp.forEach { (k, v) -> data[k] = v }
        }
    }
}
