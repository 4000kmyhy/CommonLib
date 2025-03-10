package com.lib.game2048

import android.content.Context

class GameSetting {

    companion object {
        private var instance: GameSetting? = null

        @JvmStatic
        fun getInstance(): GameSetting {
            if (instance == null) {
                synchronized(GameSetting::class.java) {
                    instance = GameSetting()
                }
            }
            return instance!!
        }

        private const val SP_NAME = "GameSetting"

        fun getSharedPreferences(context: Context) =
            context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)

        private fun getSP(context: Context?, key: String, defValue: Any): Any {
            if (context == null) return defValue
            val sp = getSharedPreferences(context)
            when (defValue) {
                is String -> {
                    return sp.getString(key, defValue) ?: defValue
                }

                is Boolean -> {
                    return sp.getBoolean(key, defValue)
                }

                is Int -> {
                    return sp.getInt(key, defValue)
                }

                is Long -> {
                    return sp.getLong(key, defValue)
                }

                is Float -> {
                    return sp.getFloat(key, defValue)
                }

                else -> return defValue
            }
        }

        private fun setSP(context: Context?, key: String, value: Any) {
            if (context == null) return
            val sp = getSharedPreferences(context)
            when (value) {
                is String -> {
                    sp.edit().putString(key, value).apply()
                }

                is Boolean -> {
                    sp.edit().putBoolean(key, value).apply()
                }

                is Int -> {
                    sp.edit().putInt(key, value).apply()
                }

                is Long -> {
                    sp.edit().putLong(key, value).apply()
                }

                is Float -> {
                    sp.edit().putFloat(key, value).apply()
                }
            }
        }

        private const val KEY_IS_SOUND_ON = "KEY_IS_SOUND_ON"//是否开启音效
        private const val KEY_SQUARE_NUM = "KEY_SQUARE_NUM"//方块数量
        private const val KEY_IS_SHOW_AI = "KEY_IS_SHOW_AI" // 显示AI
        private const val KEY_IS_SHOW_CHEAT = "KEY_IS_SHOW_CHEAT" // 显示移除数字2
        private const val KEY_ADD_TILE_NUM = "KEY_ADD_TILE_NUM"//每次移动增加数字数量
        private const val KEY_POG_2 = "KEY_POG_2"//生成数字2的概率
        private const val KEY_LIMIT_UNDO_TIMES = "KEY_LIMIT_UNDO_TIMES" //限制撤回次数
        private const val KEY_UNDO_ONCE = "KEY_LIMIT_UNDO_TIMES" //每次操作仅撤回一次
        private const val KEY_AI_INTERVAL_TIME = "KEY_AI_INTERVAL_TIME" //AI思考间隔
        private const val KEY_AI_DEPTH_LEVEL = "KEY_AI_DEPTH_LEVEL" //AI思考深度

        private const val KEY_IS_LOCK_SQUARE_ = "KEY_IS_LOCK_SQUARE_"//是否锁住方块数

        fun isLock(context: Context?, num: Int): Boolean {
            return if (num > 4) {
                return getSP(context, KEY_IS_LOCK_SQUARE_ + num, true) as Boolean
            } else {
                return false
            }
        }

        fun unlock(context: Context?, num: Int) {
            setSP(context, KEY_IS_LOCK_SQUARE_ + num, false)
        }
    }

    private var controller: GameController? = null

    var isSoundOn = true
        private set
    var squareNum = 4
        private set
    var isShowAi = true
        private set
    var isShowCheat = true
        private set
    var addTileNum = 1
        private set
    var pog2 = 90
        private set
    var limitUndoTimes = false
        private set
    var undoOnce = false
        private set

    var intervalTime = 100L
        private set
    var depthLevel = 5
        private set

    fun init(context: Context, controller: GameController) {
        this.controller = controller

        isSoundOn = getSP(context, KEY_IS_SOUND_ON, true) as Boolean
        squareNum = getSP(context, KEY_SQUARE_NUM, 4) as Int
        isShowAi = getSP(context, KEY_IS_SHOW_AI, true) as Boolean
        isShowCheat = getSP(context, KEY_IS_SHOW_CHEAT, true) as Boolean
        addTileNum = getSP(context, KEY_ADD_TILE_NUM, 1) as Int
        pog2 = getSP(context, KEY_POG_2, 90) as Int
        limitUndoTimes = getSP(context, KEY_LIMIT_UNDO_TIMES, false) as Boolean
        undoOnce = getSP(context, KEY_UNDO_ONCE, false) as Boolean

        intervalTime = getSP(context, KEY_AI_INTERVAL_TIME, 100L) as Long
        depthLevel = getSP(context, KEY_AI_DEPTH_LEVEL, 5) as Int
    }

    fun setSoundOn(context: Context?, b: Boolean) {
        if (isSoundOn != b) {
            isSoundOn = b
            setSP(context, KEY_IS_SOUND_ON, b)
        }
    }

    fun setSquareNum(context: Context?, num: Int) {
        if (squareNum != num && num >= 4) {
            squareNum = num
            setSP(context, KEY_SQUARE_NUM, num)
        }
    }

    fun setShowAi(context: Context?, b: Boolean) {
        if (isShowAi != b) {
            isShowAi = b
            setSP(context, KEY_IS_SHOW_AI, b)

            controller?.updateAiVisible()
        }
    }

    fun setShowCheat(context: Context?, b: Boolean) {
        if (isShowCheat != b) {
            isShowCheat = b
            setSP(context, KEY_IS_SHOW_CHEAT, b)

            controller?.updateCheatVisible()
        }
    }

    fun setAddTileNum(context: Context?, num: Int) {
        if (addTileNum != num) {
            addTileNum = num
            setSP(context, KEY_ADD_TILE_NUM, num)
        }
    }

    fun setPog2(context: Context?, p: Int) {
        if (pog2 != p) {
            pog2 = p
            setSP(context, KEY_POG_2, p)
        }
    }

    fun setLimitUndoTimes(context: Context?, b: Boolean) {
        if (limitUndoTimes != b) {
            limitUndoTimes = b
            setSP(context, KEY_LIMIT_UNDO_TIMES, b)

            controller?.updateUndoTimes()
        }
    }

    fun setUndoOnce(context: Context?, b: Boolean) {
        if (undoOnce != b) {
            undoOnce = b
            setSP(context, KEY_UNDO_ONCE, b)
        }
    }

    fun setIntervalTime(context: Context?, time: Long) {
        if (intervalTime != time) {
            intervalTime = time
            setSP(context, KEY_AI_INTERVAL_TIME, time)
        }
    }

    fun setDepthLevel(context: Context?, level: Int) {
        if (depthLevel != level) {
            depthLevel = level
            setSP(context, KEY_AI_DEPTH_LEVEL, level)
        }
    }
}