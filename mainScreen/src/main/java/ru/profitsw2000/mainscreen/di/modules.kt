package ru.profitsw2000.mainscreen.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import ru.profitsw2000.data.data.bluetooth.BluetoothPacketManagerImpl
import ru.profitsw2000.data.data.pll.PLLRegisters1208PL1URepositoryImpl
import ru.profitsw2000.data.data.state.RchmDissStateRepositoryImpl
import ru.profitsw2000.data.domain.bluetooth.BluetoothPacketManager
import ru.profitsw2000.data.domain.pll.PLLRegisters1208PL1URepository
import ru.profitsw2000.data.domain.state.RchmDissStateRepository
import ru.profitsw2000.mainscreen.presentation.viewmodel.MainViewModel

val mainModule = module {
    single<RchmDissStateRepository> { RchmDissStateRepositoryImpl() }
    single<BluetoothPacketManager> { BluetoothPacketManagerImpl(get(), get()) }
    single<PLLRegisters1208PL1URepository> { PLLRegisters1208PL1URepositoryImpl() }
    viewModel { MainViewModel(get(), get(), get(), get()) }
}