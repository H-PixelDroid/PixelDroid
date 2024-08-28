package org.pixeldroid.app.settings

import android.content.Context
import android.widget.EditText
import androidx.lifecycle.ViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.pixeldroid.app.utils.Tab
import org.pixeldroid.app.utils.db.AppDatabase
import org.pixeldroid.app.utils.db.entities.TabsDatabaseEntity
import org.pixeldroid.app.utils.loadDbMenuTabs
import javax.inject.Inject

@HiltViewModel
class ArrangeTabsViewModel @Inject constructor(
    private val db: AppDatabase
): ViewModel() {

    private val _uiState = MutableStateFlow(ArrangeTabsUiState())
    val uiState: StateFlow<ArrangeTabsUiState> = _uiState

    private var oldTabsChecked: MutableList<Pair<Tab, Boolean>> = mutableListOf()
    private var oldTabsButtons: MutableList<String?> = mutableListOf()

    init {
        initTabsDbEntities()
    }

    private fun initTabsDbEntities() {
        val user = db.userDao().getActiveUser()!!
        _uiState.update { currentUiState ->
            currentUiState.copy(
                userId = user.user_id,
                instanceUri = user.instance_uri,
            )
        }
        _uiState.update { currentUiState ->
            currentUiState.copy(
                tabsDbEntities = db.tabsDao().getTabsChecked(_uiState.value.userId, _uiState.value.instanceUri)
            )
        }
    }

    fun initTabsChecked(): Int {
        if (oldTabsChecked.isEmpty()) {
            // Only load tabsChecked if the model has not been updated
            _uiState.update { currentUiState ->
                currentUiState.copy(
                    tabsChecked = if (_uiState.value.tabsDbEntities.isEmpty()) {
                        // Load default menu
                        Tab.defaultTabs.zip(List(Tab.defaultTabs.size){true}) + Tab.otherTabs.zip(List(Tab.otherTabs.size){false})
                    } else {
                        // Get current menu visibility and order from settings
                        loadDbMenuTabs(_uiState.value.tabsDbEntities).toList()
                    }
                )
            }
        }
        return _uiState.value.tabsChecked.size
    }

    fun initTabsButtons(itemCount: Int, ctx: Context) {
        oldTabsChecked = _uiState.value.tabsChecked.toMutableList()
        if (oldTabsButtons.isEmpty()) {
            _uiState.update { currentUiState ->
                currentUiState.copy(
                    tabsButtons = (0 until itemCount).map { null }
                )
            }
        }
    }

    fun tabsCheckReplace(position: Int, pair: Pair<Tab, Boolean>) {
        oldTabsChecked = _uiState.value.tabsChecked.toMutableList()
        oldTabsChecked[position] = pair
        _uiState.update { currentUiState ->
            currentUiState.copy(
                tabsChecked = oldTabsChecked.toList()
            )
        }
    }

    fun tabsCheckedRemove(position: Int): Pair<Tab, Boolean> {
        oldTabsChecked = _uiState.value.tabsChecked.toMutableList()
        val removedPair = oldTabsChecked.removeAt(position)
        _uiState.update { currentUiState ->
            currentUiState.copy(
                tabsChecked = oldTabsChecked.toList()
            )
        }
        return removedPair
    }

    fun tabsCheckedAdd(position: Int, pair: Pair<Tab, Boolean>) {
        oldTabsChecked = _uiState.value.tabsChecked.toMutableList()
        oldTabsChecked.add(position, pair)
        _uiState.update { currentUiState ->
            currentUiState.copy(
                tabsChecked = oldTabsChecked.toList()
            )
        }
    }

    fun updateTabsButtons(position: Int, text: String) {
        oldTabsButtons = _uiState.value.tabsButtons.toMutableList()
        oldTabsButtons[position] = text
        _uiState.update { currentUiState ->
            currentUiState.copy(
                tabsButtons = oldTabsButtons.toList()
            )
        }
    }

    fun getTabsButtons(position: Int): String? {
        return _uiState.value.tabsButtons[position]
    }
}

data class ArrangeTabsUiState(
    val userId: String = "",
    val instanceUri: String = "",
    val tabsDbEntities: List<TabsDatabaseEntity> = listOf(),
    val tabsChecked: List<Pair<Tab, Boolean>> = listOf(),
    val tabsButtons: List<String?> = listOf()
)