package com.kreedaankana.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FirebaseFirestore
import com.kreedaankana.R
import com.kreedaankana.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Handle insets for bottom navigation
        binding.bottomNav.setOnApplyWindowInsetsListener { view, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNav.setupWithNavController(navController)
        
        // Handle insets for App Bar
        binding.appBar.setOnApplyWindowInsetsListener { view, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        // Check for app updates asynchronously from Firestore
        checkAppUpdate()
    }

    private fun checkAppUpdate() {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("app_settings").document("version_config")
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val latestVersionCode = document.getLong("latest_version_code") ?: 1L
                    val latestVersionName = document.getString("latest_version_name") ?: "1.0.0"
                    val forceUpdate = document.getBoolean("force_update") ?: false
                    val updateUrl = document.getString("update_url") ?: "https://github.com/Namith-kp/Kreeda-Ankana/releases"

                    val packageInfo = try {
                        packageManager.getPackageInfo(packageName, 0)
                    } catch (e: Exception) {
                        null
                    }
                    val localVersionCode = packageInfo?.let {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                            it.longVersionCode
                        } else {
                            @Suppress("DEPRECATION")
                            it.versionCode.toLong()
                        }
                    } ?: 1L

                    if (latestVersionCode > localVersionCode) {
                        showUpdateDialog(latestVersionName, forceUpdate, updateUrl)
                    }
                }
            }
            .addOnFailureListener {
                // Ignore failure silently so the app functions normally if offline
            }
    }

    private fun showUpdateDialog(latestVersionName: String, forceUpdate: Boolean, updateUrl: String) {
        val dialogBuilder = MaterialAlertDialogBuilder(this)
            .setTitle("🚀 New Update Available!")
            .setMessage("A premium new version ($latestVersionName) of Kreeda Ankana is available with exciting new features, matches, bookings, and performance improvements.\n\nPlease update to enjoy the best experience!")
            .setPositiveButton("Update Now") { _, _ ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl))
                    startActivity(intent)
                } catch (e: Exception) {
                    // Fallback
                }
                if (forceUpdate) {
                    showUpdateDialog(latestVersionName, forceUpdate, updateUrl)
                }
            }

        if (!forceUpdate) {
            dialogBuilder.setNegativeButton("Later", null)
        }

        val dialog = dialogBuilder.create()
        dialog.setCancelable(!forceUpdate)
        dialog.setCanceledOnTouchOutside(!forceUpdate)
        dialog.show()
    }
}
