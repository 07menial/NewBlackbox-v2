package com.example.multicloner

import android.app.Activity
import android.content.Context
import android.os.Build
import android.widget.Toast
import top.niunaijun.blackbox.BlackBoxCore

/** Motorul care face efectiv clonarea. UI-ul vorbeste doar cu aceasta interfata. */
interface CloneEngine {
    /** true daca motorul izoleaza datele clonei (cont separat). */
    val isRealClone: Boolean
    /** Returneaza null la succes, sau mesajul de eroare. */
    fun install(context: Context, pkg: String, userId: Int): String?
    fun launch(context: Context, pkg: String, userId: Int): Boolean
    fun uninstall(context: Context, pkg: String, userId: Int)
    fun hasFileAccess(): Boolean = true
    fun requestFileAccess(activity: Activity) {}
}

/** Motor real: virtualizare prin BlackBox (modulul :Bcore, din NewBlackbox). */
class BlackBoxEngine : CloneEngine {
    override val isRealClone = true

    override fun install(context: Context, pkg: String, userId: Int): String? = try {
        val result = BlackBoxCore.get().installPackageAsUser(pkg, userId)
        if (result.success) null else (result.msg ?: "Instalare esuata")
    } catch (e: Throwable) {
        e.message ?: "Eroare necunoscuta"
    }

    override fun launch(context: Context, pkg: String, userId: Int): Boolean {
        val ok = try {
            BlackBoxCore.get().launchApk(pkg, userId)
        } catch (e: Throwable) {
            false
        }
        if (!ok) Toast.makeText(context, "Nu am putut porni clona", Toast.LENGTH_SHORT).show()
        return ok
    }

    override fun uninstall(context: Context, pkg: String, userId: Int) {
        runCatching { BlackBoxCore.get().uninstallPackageAsUser(pkg, userId) }
    }

    override fun hasFileAccess(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.R ||
            runCatching { BlackBoxCore.get().hasAllFilesAccess() }.getOrDefault(true)

    override fun requestFileAccess(activity: Activity) {
        runCatching { BlackBoxCore.get().requestAllFilesAccess(activity) }
    }
}

object Engine {
    val current: CloneEngine = BlackBoxEngine()
}
