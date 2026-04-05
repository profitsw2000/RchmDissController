package ru.profitsw2000.rchmdisscontroller.navigator

import androidx.navigation.NavController
import ru.profitsw2000.navigator.Navigator
import ru.profitsw2000.rchmdisscontroller.R

class NavigatorImpl(private val navController: NavController): Navigator{
    override fun navigateToTransmitterSettingsDialog() {
        navController.navigate(R.id.action_main_to_transmitter_settings_dialog_fragment)
    }

    override fun navigateToReceiverSettingsDialog() {
        navController.navigate(R.id.action_main_to_receiver_settings_dialog_fragment)
    }

    override fun navigateToSynthesizerSettingsDialog() {
        navController.navigate(R.id.action_main_to_synthesizer_settings_dialog_fragment)
    }
}