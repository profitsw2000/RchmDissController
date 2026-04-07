package ru.profitsw2000.rchmdisscontroller.di

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import ru.profitsw2000.data.data.bluetooth.BluetoothRepositoryImpl
import ru.profitsw2000.data.domain.bluetooth.BluetoothRepository
import ru.profitsw2000.rchmdisscontroller.presentation.viewmodel.MainActivityViewModel

val appModule = module {
    single<BluetoothRepository> { BluetoothRepositoryImpl(androidContext()) }
    viewModel { MainActivityViewModel(get()) }
}