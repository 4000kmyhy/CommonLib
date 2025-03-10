package com.lib.game2048

interface IBindGameListener {
    fun getUndoTimesByAD(undo: () -> Unit)
    fun cheatByAD(cheat: () -> Unit)
    fun runAiByAD(runAi: () -> Unit)
}