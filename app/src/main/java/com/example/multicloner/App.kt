package com.example.multicloner

import android.app.Application
import android.content.Context
import android.util.Log
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackbox.app.configuration.ClientConfiguration
import java.io.File

/**
 * Initializeaza motorul BlackBox. Ordinea (attachBaseContext -> onCreate) e ceruta de motor;
 * ruleaza si in procesele ":black" in care se executa aplicatiile clonate.
 */
class App : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        try {
            BlackBoxCore.get().closeCodeInit()
            BlackBoxCore.get().onBeforeMainApplicationAttach(this, base)
            BlackBoxCore.get().doAttachBaseContext(base, object : ClientConfiguration() {
                override fun getHostPackageName(): String = base.packageName
                override fun isHideRoot(): Boolean = false
                override fun isEnableDaemonService(): Boolean = false
                override fun isUseVpnNetwork(): Boolean = false
                override fun isDisableFlagSecure(): Boolean = false
                override fun requestInstallPackage(file: File?, userId: Int): Boolean = false
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
