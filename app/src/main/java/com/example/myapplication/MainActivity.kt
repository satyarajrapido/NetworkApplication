package com.example.myapplication

import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.myapplication.databinding.ActivityMainBinding
import java.time.LocalDate
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            askUsageAccessPermission()
        }

        binding.fab.setOnClickListener { view ->

            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 15) // set to 3 PM
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            val startTime = calendar.timeInMillis

            Log.d("TAG", "startTime: $startTime")

            calendar.set(Calendar.HOUR_OF_DAY, 17) // set to 5 PM
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            val endTime = calendar.timeInMillis

            Log.d("TAG", "endTime: $endTime")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getNetworkUsageForApp("com.rapido.passenger", startTime, endTime).let {
                    Log.d("TAG", "USAGE: $it")
                }
            }else{
                Toast.makeText(this, "Not supported OS version", Toast.LENGTH_LONG).show()
            }

        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun askUsageAccessPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ( !getUsageAccessPermission()) {
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                try {
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    val intentCrash = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    startActivity(intentCrash)
                    Toast.makeText(this, "Not able to find the specific app, Please grant usage access permission manually", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun getUsageAccessPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(), packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun getNetworkUsageForApp(packageName: String, startTime: Long, endTime: Long): Long {
        val packageManager = packageManager
        val uid = packageManager.getApplicationInfo(packageName, 0).uid
        val networkStatsManager = getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager

        val mobileBucket = networkStatsManager.queryDetailsForUid(
            NetworkCapabilities.TRANSPORT_CELLULAR,
            "",
            startTime,
            endTime, uid)// uid


        val wifiBucket = networkStatsManager.queryDetailsForUid(
            NetworkCapabilities.TRANSPORT_WIFI,
            "",
            startTime,
            endTime,
            uid)


        var mobileRxBytes = 0L
        var mobileTxBytes = 0L

        var wifiRxBytes = 0L
        var wifiTxBytes = 0L

        val bucketUsage = NetworkStats.Bucket()

        while (mobileBucket.hasNextBucket()) {
            mobileBucket.getNextBucket(bucketUsage)
            mobileRxBytes += bucketUsage.rxBytes
            mobileTxBytes += bucketUsage.txBytes
        }
        mobileBucket.close()

        while (wifiBucket.hasNextBucket()) {
            wifiBucket.getNextBucket(bucketUsage)
            wifiRxBytes += bucketUsage.rxBytes
            wifiTxBytes += bucketUsage.txBytes
        }

        wifiBucket.close()

        val mobileBytes = mobileRxBytes + mobileTxBytes
        val wifiBytes = wifiRxBytes + wifiTxBytes

        Log.d("TAG", "BYTES: $mobileBytes + $wifiBytes")

        return  convertBytesToMB(mobileBytes) + convertBytesToMB(wifiBytes)
    }

    //create a function to accept the bytes and convert it to MB
    private fun convertBytesToMB(bytes: Long): Long {
        return bytes / 1024 / 1024
    }

    private fun isPackageInstalled(packageName: String, packageManager: PackageManager): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("TAG", "isPackageInstalled: $e")
            false
        }
    }
}