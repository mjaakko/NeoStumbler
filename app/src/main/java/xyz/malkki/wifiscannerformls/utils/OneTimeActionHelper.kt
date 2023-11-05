package xyz.malkki.wifiscannerformls.utils

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import xyz.malkki.wifiscannerformls.StumblerApplication

class OneTimeActionHelper(app: StumblerApplication) {
    private val oneTimeActionsStore = app.oneTimeActionsStore

    /**
     * Checks if the specified action has already been shown to the user
     *
     * @param actionName Action name
     * @return true if the action has been shown, false if not
     */
    suspend fun hasActionBeenShown(actionName: String): Boolean {
        return oneTimeActionsStore.data
            .map { it[booleanPreferencesKey(actionName)] }
            .firstOrNull() == true
    }

    /**
     * Marks that the specified action has bene shown to the user
     *
     * @param actionName Action name
     */
    suspend fun markActionShown(actionName: String) {
        oneTimeActionsStore.edit { it[booleanPreferencesKey(actionName)] = true }
    }
}