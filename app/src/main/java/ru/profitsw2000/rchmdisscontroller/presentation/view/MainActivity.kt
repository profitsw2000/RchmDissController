package ru.profitsw2000.rchmdisscontroller.presentation.view

import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
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
                Toast.makeText(this, "Clicked menu item", Toast.LENGTH_SHORT).show()
                mainActivityViewModel.switchBluetooth()
                true
            }
            ru.profitsw2000.core.R.id.bluetooth_connection_status -> true
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
                mainActivityViewModel.permissionIsDenied.collect { event ->
                    if (event && shouldShowRequestPermissionRationale(android.Manifest.permission.BLUETOOTH_CONNECT)) showRationaleDialog()
                }
            }
        }
    }

    private fun showRationaleDialog() {
        showSimpleDialog(messageTitle = "Разрешение Bluetooth",
            messageText = "Для обмена информацией с РЧМ-ДИСС необходимо включить Bluetooth",
            buttonText = "OK")
    }

    private fun showSimpleDialog(messageTitle: String,
                                 messageText: String,
                                 buttonText: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(messageTitle)
            .setMessage(messageText)
            .setNeutralButton(buttonText) { dialog, _ -> dialog.dismiss()}
            .create()
            .show()
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