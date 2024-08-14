package org.pixeldroid.app.utils.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import org.pixeldroid.app.utils.db.entities.TabsDatabaseEntity

@Dao
interface TabsDao {
    @Query("SELECT * FROM tabsChecked WHERE `index`=:index")
    fun getTabChecked(index: Int): TabsDatabaseEntity

    @Query("SELECT * FROM tabsChecked")
    fun getTabsChecked(): List<TabsDatabaseEntity>

    @Query("DELETE FROM tabsChecked WHERE `index`=:index")
    fun deleteTabChecked(index: Int)

    @Query("DELETE FROM tabsChecked")
    fun deleteTabsChecked()

    /**
     * Insert an instance, if it already exists return -1
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTabChecked(tabChecked: TabsDatabaseEntity): Long

    @Update
    suspend fun updateTabChecked(tabChecked: TabsDatabaseEntity)

    @Transaction
    suspend fun insertOrUpdate(tabChecked: TabsDatabaseEntity) {
        if (insertTabChecked(tabChecked) == -1L) {
            updateTabChecked(tabChecked)
        }
    }

    @Transaction
    suspend fun clearAndRefill(tabsChecked: List<TabsDatabaseEntity>) {
        deleteTabsChecked()
        tabsChecked.forEach { insertTabChecked(it) }
    }
}