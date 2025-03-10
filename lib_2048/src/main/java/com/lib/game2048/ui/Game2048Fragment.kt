package com.lib.game2048.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import com.lib.game2048.GameController
import com.lib.game2048.GameSetting
import com.lib.game2048.IBindGameListener
import com.lib.game2048.R
import com.lib.game2048.databinding.FragmentGame2048Binding
import kotlinx.coroutines.launch

class Game2048Fragment : Fragment() {

    companion object {
        fun newInstance(isPro: Boolean) = Game2048Fragment().apply {
            arguments = Bundle().apply {
                putBoolean("isPro", isPro)
            }
        }

        fun replaceFragment(
            fragmentManager: FragmentManager,
            @IdRes containerViewId: Int,
            isPro: Boolean
        ) {
            fragmentManager.beginTransaction()
                .replace(containerViewId, newInstance(isPro), "Game2048Fragment")
                .commitAllowingStateLoss()
        }
    }

    private var bindGameListener: IBindGameListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is IBindGameListener) {
            bindGameListener = context
        }
    }

    private lateinit var binding: FragmentGame2048Binding
    private var isPro = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentGame2048Binding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isPro = arguments?.getBoolean("isPro", false) ?: false

        lifecycleScope.launch {
            withResumed {
                binding.gameView.controller.load()
            }
        }

        initView()
        initData()
        initEvent()

    }

    private fun initView() {
        binding.gameLayout.bindController(binding.gameView.controller)
        binding.gameView.controller.setOnScoreUpdatedListener(object :
            GameController.OnScoreUpdatedListener {
            override fun onScoreUpdated(score: Long) {
                binding.tvScore.text = "$score"
            }

            override fun onHighScoreUpdated(score: Long) {
                binding.tvMaxScore.text = "$score"
            }

            override fun onUndoTimesUpdated(times: Int) {
                if (isPro) {
                    if (GameSetting.getInstance().limitUndoTimes) {
                        binding.undoBadgeView.isVisible = true
                        binding.undoBadgeView.setValue(times)
                    } else {
                        binding.undoBadgeView.isVisible = false
                    }
                } else {
                    if (GameSetting.getInstance().limitUndoTimes) {
                        binding.undoBadgeView.isVisible = true
                        binding.undoBadgeView.setValue(times)
                    } else {
                        binding.undoBadgeView.isVisible = true
                        if (times > 0) {
                            binding.undoBadgeView.setValue(times)
                        } else {
                            binding.undoBadgeView.setValue("AD")
                        }
                    }
                }
            }

            override fun onAiVisibleUpdated() {
                binding.btnAi.isVisible = GameSetting.getInstance().isShowAi
                binding.aiBadgeView.isVisible = !isPro && GameSetting.getInstance().isShowAi
            }

            override fun onCheatVisibleUpdated() {
                binding.btnCheat.isVisible = GameSetting.getInstance().isShowCheat
                binding.cheatBadgeView.isVisible = !isPro && GameSetting.getInstance().isShowCheat
            }
        })
    }

    private fun initData() {
        binding.btnSound.isSelected = GameSetting.getInstance().isSoundOn

        binding.btnAi.isVisible = GameSetting.getInstance().isShowAi
        binding.aiBadgeView.isVisible = !isPro && GameSetting.getInstance().isShowAi
        binding.aiBadgeView.setValue("AD")

        binding.btnCheat.isVisible = GameSetting.getInstance().isShowCheat
        binding.cheatBadgeView.isVisible = !isPro && GameSetting.getInstance().isShowCheat
        binding.cheatBadgeView.setValue("AD")
    }

    private fun initEvent() {
        binding.btnRefresh.setOnClickListener {
            binding.gameView.controller.newGame()
        }
        binding.btnUndo.setOnClickListener {
            if (isPro) {
                if (GameSetting.getInstance().limitUndoTimes) {
                    if (binding.gameView.controller.undoTimes > 0) {
                        binding.gameView.controller.revertUndoState()
                    }
                } else {
                    binding.gameView.controller.revertUndoState()
                }
            } else {
                if (GameSetting.getInstance().limitUndoTimes) {
                    if (binding.gameView.controller.undoTimes > 0) {
                        binding.gameView.controller.revertUndoState()
                    }
                } else {
                    if (binding.gameView.controller.undoTimes > 0) {
                        binding.gameView.controller.revertUndoState()
                    } else {
                        bindGameListener?.getUndoTimesByAD {
                            binding.gameView.controller.resetUndoTimes()
                            Toast.makeText(
                                context,
                                R.string.undo_times_increase,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
        binding.btnCheat.setOnClickListener {
            if (isPro) {
                binding.gameView.controller.cheat()
            } else {
                bindGameListener?.cheatByAD {
                    binding.gameView.controller.cheat()
                }
            }
        }
        binding.btnAi.setOnClickListener {
            if (isPro) {
                binding.gameView.controller.runAi()
            } else {
                bindGameListener?.runAiByAD {
                    binding.gameView.controller.runAi()
                }
            }
        }
        binding.btnSound.setOnClickListener {
            val isSelected = !it.isSelected
            it.isSelected = isSelected
            GameSetting.getInstance().setSoundOn(context, isSelected)
        }
    }

    override fun onPause() {
        super.onPause()
        binding.gameView.controller.stopAi()
        binding.gameView.controller.save()
    }
}