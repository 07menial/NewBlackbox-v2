package com.example.multicloner

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Folder(val id: String, val name: String)

data class Clone(
    val id: String,
    val pkg: String,
    val label: String,
    val userId: Int,          // "spatiul" virtual in care ruleaza clona
    val folderId: String?     // null = fara folder
)

/** Stocare locala simpla (SharedPreferences + JSON). */
class Store(context: Context) {
    private val prefs = context.getSharedPreferences("multicloner", Context.MODE_PRIVATE)
    val folders = mutableStateListOf<Folder>()
    val clones = mutableStateListOf<Clone>()

    /**
     * Stocarea vazuta de clone (citita si de App.kt in procesele clonelor):
     * 0 = separata pentru fiecare clona, 1 = comuna pentru toate clonele, 2 = stocarea reala a telefonului.
     */
    var storageMode by mutableIntStateOf(prefs.getInt(KEY_STORAGE_MODE, 1))
        private set

    fun setStorageMode(mode: Int) {
        storageMode = mode
        prefs.edit().putInt(KEY_STORAGE_MODE, mode).commit()
    }

    companion object {
        const val PREFS = "multicloner"
        const val KEY_STORAGE_MODE = "storage_mode"
        const val DEFAULT_STORAGE_MODE = 1
    }

    init {
        runCatching {
            val f = JSONArray(prefs.getString("folders", "[]"))
            for (i in 0 until f.length()) {
                val o = f.getJSONObject(i)
                folders.add(Folder(o.getString("id"), o.getString("name")))
            }
            val c = JSONArray(prefs.getString("clones", "[]"))
            for (i in 0 until c.length()) {
                val o = c.getJSONObject(i)
                clones.add(
                    Clone(
                        o.getString("id"), o.getString("pkg"), o.getString("label"),
                        o.getInt("userId"),
                        if (o.isNull("folderId")) null else o.getString("folderId")
                    )
                )
            }
        }
    }

    private fun save() {
        val f = JSONArray()
        folders.forEach { f.put(JSONObject().put("id", it.id).put("name", it.name)) }
        val c = JSONArray()
        clones.forEach {
            c.put(
                JSONObject().put("id", it.id).put("pkg", it.pkg).put("label", it.label)
                    .put("userId", it.userId).put("folderId", it.folderId ?: JSONObject.NULL)
            )
        }
        prefs.edit().putString("folders", f.toString()).putString("clones", c.toString()).apply()
    }

    fun addFolder(name: String) {
        folders.add(Folder(UUID.randomUUID().toString(), name.trim()))
        save()
    }

    fun renameFolder(id: String, name: String) {
        val i = folders.indexOfFirst { it.id == id }
        if (i >= 0) folders[i] = folders[i].copy(name = name.trim())
        save()
    }

    /** Stergerea unui folder muta clonele lui in "Fara folder" (nu le sterge). */
    fun deleteFolder(id: String) {
        for (i in clones.indices) if (clones[i].folderId == id) clones[i] = clones[i].copy(folderId = null)
        folders.removeAll { it.id == id }
        save()
    }

    /** Urmatorul userId liber pentru un pachet (fiecare clona a aceluiasi app are alt userId). */
    fun nextUserId(pkg: String): Int {
        val used = clones.filter { it.pkg == pkg }.map { it.userId }.toSet()
        var id = 0
        while (id in used) id++
        return id
    }

    fun addClone(pkg: String, label: String, userId: Int, folderId: String?) {
        clones.add(Clone(UUID.randomUUID().toString(), pkg, label, userId, folderId))
        save()
    }

    fun moveClone(id: String, folderId: String?) {
        val i = clones.indexOfFirst { it.id == id }
        if (i >= 0) clones[i] = clones[i].copy(folderId = folderId)
        save()
    }

    fun removeClone(id: String) {
        clones.removeAll { it.id == id }
        save()
    }
}
