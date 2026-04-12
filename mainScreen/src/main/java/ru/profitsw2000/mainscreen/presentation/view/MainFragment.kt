package ru.profitsw2000.mainscreen.presentation.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.koin.android.ext.android.inject
import ru.profitsw2000.mainscreen.R
import ru.profitsw2000.mainscreen.databinding.FragmentMainBinding
import ru.profitsw2000.navigator.Navigator

class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private val navigator: Navigator by inject()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentMainBinding.bind(inflater.inflate(R.layout.fragment_main, container, false))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    private fun initViews() = with(binding) {
        transmitterConstraintLayout.setOnClickListener {
            navigator.navigateToTransmitterSettingsDialog()
        }
        receiverConstraintLayout.setOnClickListener {
            navigator.navigateToReceiverSettingsDialog()
        }
        synthesizerConstraintLayout.setOnClickListener {
            navigator.navigateToSynthesizerSettingsDialog()
        }
    }
}


/*private fun List<View>.setInteractions(enabled: Boolean) {
    forEach {
        it.isEnabled = enabled
        it.alpha = if (enabled) 1f else 0.5f // визуальный отклик
    }
}

// Теперь в коде это выглядит очень просто:
val myLayouts = listOf(binding.layout1, binding.layout2, binding.layout3)

// Когда началось подключение:
myLayouts.setInteractions(false)

// Когда подключение завершилось:
myLayouts.setInteractions(true)
Используйте код с осторожностью.

Важный момент:
Если вы используете Coroutines (корутины) для подключения, не забудьте поместить включение обратно в блок finally. Это гарантирует, что лэйауты разблокируются, даже если произойдет ошибка сети:
kotlin
viewLifecycleOwner.lifecycleScope.launch {
    try {
        myLayouts.setInteractions(false)
        // Логика подключения...
    } catch (e: Exception) {
        // Обработка ошибки
    } finally {
        // Выполнится в любом случае (успех или ошибка)
        myLayouts.setInteractions(true)
    }
}
Используйте код с осторожностью.

Вы используете Coroutines или Callbacks (слушатели) для обработки процесса подключения?*/




