package com.lib.eqpreset

import android.content.Context
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONException

/**
 * desc:
 **
 * user: xujj
 * time: 2023/6/6 9:59
 **/
@Parcelize
data class EqPreset(
    val name: String,
    val eqValue: IntArray
) : Parcelable {

    companion object {
        fun getEqPresetList(context: Context): ArrayList<EqPreset> {
            val presetList = ArrayList<EqPreset>()
            val presetNames = context.resources.getStringArray(R.array.eq_preset_name)
            val presetValues = context.resources.getStringArray(R.array.eq_preset_value)
            for (i in presetNames.indices) {
                val eqValue = IntArray(10)
                try {
                    val jsonArray = JSONArray(presetValues[i])
                    for (j in 0 until jsonArray.length()) {
                        eqValue[j] = jsonArray.getInt(j)
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
                presetList.add(EqPreset(presetNames[i], eqValue))
            }
            return presetList
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EqPreset

        if (!eqValue.contentEquals(other.eqValue)) return false

        return true
    }

    override fun hashCode(): Int {
        return eqValue.contentHashCode()
    }

    override fun toString(): String {
        return "EqPreset(name='$name', eqValue=${eqValue.contentToString()})"
    }
}