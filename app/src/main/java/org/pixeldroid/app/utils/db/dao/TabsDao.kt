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
    @Query("SELECT * FROM tabsChecked WHERE `index`=:index AND `user_id`=:userId AND `instance_uri`=:instanceUri")
    fun getTabChecked(index: Int, userId: String, instanceUri: String): TabsDatabaseEntity

    @Query("SELECT * FROM tabsChecked WHERE `user_id`=:userId AND `instance_uri`=:instanceUri")
    fun getTabsChecked(userId: String, instanceUri: String): List<TabsDatabaseEntity>

    @Query("DELETE FROM tabsChecked WHERE `index`=:index AND `user_id`=:userId AND `instance_uri`=:instanceUri")
    fun deleteTabChecked(index: Int, userId: String, instanceUri: String)

    @Query("DELETE FROM tabsChecked WHERE `user_id`=:userId AND `instance_uri`=:instanceUri")
    fun deleteTabsChecked(userId: String, instanceUri: String)

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
    suspend fun clearAndRefill(tabsChecked: List<TabsDatabaseEntity>, userId: String, instanceUri: String) {
        deleteTabsChecked(userId, instanceUri)
        tabsChecked.forEach { insertTabChecked(it) }
    }
}