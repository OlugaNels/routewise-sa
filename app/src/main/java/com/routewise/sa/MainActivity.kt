package com.routewise.sa

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.routewise.sa.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    private val notificationPermissionLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Never show the app to someone who is not signed in (covers deep links and a stale session)
        if (!com.routewise.sa.data.SessionManager(this).isLoggedIn() ||
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null
        ) {
            com.routewise.sa.data.SessionManager(this).logout()
            startActivity(
                android.content.Intent(this, com.routewise.sa.ui.auth.LoginActivity::class.java).apply {
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            finish()
            return
        }

        // Android 13 and newer need the user's permission before alerts can be shown as notifications
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Toolbar
        setSupportActionBar(binding.topToolbar)

        // Navigation Controller Setup
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        // Top level destinations (Drawer + BottomNav)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_map,
                R.id.navigation_plan,
                R.id.navigation_alerts,
                R.id.navigation_report,
                R.id.navigation_profile,
                R.id.navigation_settings,
                R.id.navigation_chat,
                R.id.navigation_help
            ),
            binding.drawerLayout
        )

        // Bind Toolbar, BottomNav & Drawer with NavController
        binding.topToolbar.setupWithNavController(navController, appBarConfiguration)
        binding.bottomNavigationView.setupWithNavController(navController)
        binding.navigationView.setupWithNavController(navController)

        // The Alerts tab used to show a fixed "8" badge, which was not real data
        binding.bottomNavigationView.removeBadge(R.id.navigation_alerts)

        // Drawer custom item clicks
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.drawer_profile -> {
                    navController.navigate(R.id.navigation_profile)
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.drawer_settings -> {
                    navController.navigate(R.id.navigation_settings)
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.drawer_chat -> {
                    navController.navigate(R.id.navigation_chat)
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.drawer_help -> {
                    navController.navigate(R.id.navigation_help)
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.drawer_logout -> {
                    com.routewise.sa.data.SessionManager(this).logout()
                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                    Toast.makeText(this, "Logged out of RouteWise SA", Toast.LENGTH_SHORT).show()
                    val intent = android.content.Intent(this, com.routewise.sa.ui.auth.LoginActivity::class.java).apply {
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                else -> {
                    val handled = NavigationUI.onNavDestinationSelected(menuItem, navController)
                    if (handled) binding.drawerLayout.closeDrawer(GravityCompat.START)
                    handled
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp()
    }
}
