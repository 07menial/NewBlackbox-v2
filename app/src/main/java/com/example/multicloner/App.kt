package com.example.multicloner

import android.app.Application
import android.content.Context
import android.util.Log
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackbox.app.configuration.ClientConfiguration
import java.io.File

/**
 * Initializeaza motorul BlackBox. Ruleaza si in procesele ":black" / p0.. in care se executa
 * aplicatiile clonate, de aceea setarile se citesc direct din SharedPreferences.
 */
class App : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        try {
            val prefs = base.getSharedPreferences(Store.PREFS, Context.MODE_PRIVATE)
            BlackBoxCore.get().closeCodeInit()
            BlackBoxCore.get().onBeforeMainApplicationAttach(this, base)
            BlackBoxCore.get().doAttachBaseContext(base, object : ClientConfiguration() {
                override fun getHostPackageName(): String = base.packageName
                override fun isHideRoot(): Boolean = false
                override fun isEnableDaemonService(): Boolean = false
                override fun isUseVpnNetwork(): Boolean = false
                override fun isDisableFlagSecure(): Boolean = false
                override fun requestInstallPackage(file: File?, userId: Int): Boolean = false

                // Nicio trimitere de log-uri catre servere externe.
                override fun getLogSenderChatId(): String = ""

                override fun getExternalStorageMode(): Int =
                    prefs.getInt(Store.KEY_STORAGE_MODE, Store.DEFAULT_STORAGE_MODE)
            })
            BlackBoxCore.get().onAfterMainApplicationAttach(this, base)
        } catch (e: Throwable) {
            Log.e("MultiCloner", "BlackBox attach failed", e)
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            BlackBoxCore.get().doCreate()
        } catch (e: Throwable) {
            Log.e("MultiCloner", "BlackBox create failed", e)
        }
    }
}
