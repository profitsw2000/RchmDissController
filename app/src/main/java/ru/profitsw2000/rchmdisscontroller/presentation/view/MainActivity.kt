package ru.profitsw2000.rchmdisscontroller.presentation.view

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import ru.profitsw2000.navigator.Navigator
import ru.profitsw2000.rchmdisscontroller.R
import ru.profitsw2000.rchmdisscontroller.databinding.ActivityMainBinding
import ru.profitsw2000.rchmdisscontroller.navigator.NavigatorImpl
import ru.profitsw2000.rchmdisscontroller.presentation.viewmodel.MainActivityViewModel

class  MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController
    private lateinit var binding: ActivityMainBinding
    private val mainActivityViewModel: MainActivityViewModel by viewModel()
    private var bluetoothStateIconColor: Int = Color.GRAY


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bluetoothStateIconColor = MaterialColors.getColor(
            this,
            com.google.android.material.R.attr.colorOnSurfaceVariant,
            Color.GRAY
        )

        setSupportActionBar(binding.topAppBar)
        initBottomNavigationView()
        initBluetooth()
        observeBluetoothEnableData()
        observePermissionDeniedData()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(ru.profitsw2000.core.R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            ru.profitsw2000.core.R.id.bluetooth_state -> {
                mainActivityViewModel.switchBluetooth(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                        shouldShowRequestPermissionRationale(android.Manifest.permission.BLUETOOTH_CONNECT)
                    else false
                )
                true
            }
            ru.profitsw2000.core.R.id.bluetooth_connection_status -> {
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(ru.profitsw2000.core.R.id.bluetooth_state)?.icon?.setTint(bluetoothStateIconColor)
        return super.onPrepareOptionsMenu(menu)
    }

    private fun initBottomNavigationView() {
        val navHostFragment = supportFragmentManager.findFragmentById(
            R.id.fragment_container_view
        ) as NavHostFragment

        navController = navHostFragment.navController
        loadKoinModules( module { single<Navigator> { NavigatorImpl(navController) } })
        binding.bottomNavigationView.setupWithNavController(navController)
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.main, R.id.registers, R.id.history)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    private fun initBluetooth() {
        mainActivityViewModel.checkBluetoothState()
        mainActivityViewModel.setupRegistry(activityResultRegistry, this)
    }

    private fun observeBluetoothEnableData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainActivityViewModel.bluetoothIsEnabled.collect { isEnabled ->
                    setBluetoothStateIconColor(isEnabled)
                    invalidateOptionsMenu()
                }
            }
        }
    }

    private fun observePermissionDeniedData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainActivityViewModel.shouldShowRationale.collect { isShow ->
                    if (isShow) showRationaleDialog()
                    mainActivityViewModel.rationaleIsShowed()
                }
            }
        }
    }

    private fun showRationaleDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(ru.profitsw2000.core.R.string.bluetooth_permission_rationale_dialog_title))
            .setMessage(getString(ru.profitsw2000.core.R.string.bluetooth_permission_rationale_dialog_text))
            .setPositiveButton(getString(ru.profitsw2000.core.R.string.yes_button_text)) { dialog, _ ->
                openAppSettings()
                dialog.dismiss()
            }
            .setNegativeButton(getString(ru.profitsw2000.core.R.string.no_button_text)) {
                dialog, _ -> dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    private fun setBluetoothStateIconColor(isEnabled: Boolean) {
        bluetoothStateIconColor = if (isEnabled)
            MaterialColors.getColor(
                this,
                com.google.android.material.R.attr.colorOnSurface,
                Color.GRAY
            )
        else
            MaterialColors.getColor(
                this,
                com.google.android.material.R.attr.colorOnSurfaceVariant,
                Color.GRAY
            )
    }
}