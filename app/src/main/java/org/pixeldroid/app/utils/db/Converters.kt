package org.pixeldroid.app.utils.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.pixeldroid.app.utils.api.objects.*
import java.util.*

class Converters {
    private val gson = Gson()
    
    @TypeConverter
    fun listToJson(list: List<String>): String = gson.toJson(list)

     @TypeConverter
     fun jsonToList(json: String): List<String> =
         gson.fromJson(json, Array<String>::class.java).toList()

    @TypeConverter
    fun dateToJson(date: Date): String = gson.toJson(date)

    @TypeConverter
    fun jsonToDate(json: String): Date = gson.fromJson(json, Date::class.java)

    @TypeConverter
    fun accountToJson(account: Account): String = gson.toJson(account)

    @TypeConverter
    fun jsonToAccount(json: String): Account =  gson.fromJson(json, Account::class.java)

    @TypeConverter
    fun statusToJson(status: Status?): String = gson.toJson(status)

    @TypeConverter
    fun jsonToStatus(json: String): Status? =  gson.fromJson(json, Status::class.java)

    @TypeConverter
    fun notificationTypeToJson(type: Notification.NotificationType?): String = gson.toJson(type)

    @TypeConverter
    fun jsonToNotificationType(json: String): Notification.NotificationType? =  gson.fromJson(
        json,
        Notification.NotificationType::class.java
    )

    @TypeConverter
    fun applicationToJson(type: Application?): String = gson.toJson(type)

    @TypeConverter
    fun jsonToApplication(json: String): Application? =  gson.fromJson(
        json,
        Application::class.java
    )

    @TypeConverter
    fun cardToJson(type: Card?): String = gson.toJson(type)

    @TypeConverter
    fun jsonToCard(json: String): Card? =  gson.fromJson(json, Card::class.java)

    @TypeConverter
    fun attachmentToJson(type: Attachment?): String = gson.toJson(type)

    @TypeConverter
    fun jsonToAttachment(json: String): Attachment? =  gson.fromJson(json, Attachment::class.java)

    @TypeConverter
    fun attachmentListToJson(type: List<Attachment>?): String {
        val listType = object : TypeToken<List<Attachment?>?>() {}.type
        return gson.toJson(type, listType)
    }

    @TypeConverter
    fun jsonToAttachmentList(json: String): List<Attachment>? {
        val listType = object : TypeToken<List<Attachment?>?>() {}.type
        return gson.fromJson(json, listType)
    }

    @TypeConverter
    fun mentionListToJson(type: List<Mention>?): String {
        val listType = object : TypeToken<List<Mention?>?>() {}.type
        return gson.toJson(type, listType)
    }

    @TypeConverter
    fun jsonToMentionList(json: String): List<Mention>? {
        val listType = object : TypeToken<List<Mention?>?>() {}.type
        return gson.fromJson(json, listType)
    }

    @TypeConverter
    fun emojiListToJson(type: List<Emoji>?): String {
        val listType = object : TypeToken<List<Emoji?>?>() {}.type
        return gson.toJson(type, listType)
    }

    @TypeConverter
    fun jsonToEmojiList(json: String): List<Emoji>? {
        val listType = object : TypeToken<List<Emoji?>?>() {}.type
        return gson.fromJson(json, listType)
    }

    @TypeConverter
    fun tagListToJson(type: List<Tag>?): String {
        val listType = object : TypeToken<List<Tag?>?>() {}.type
        return gson.toJson(type, listType)
    }

    @TypeConverter
    fun jsonToTagList(json: String): List<Tag>? {
        val listType = object : TypeToken<List<Tag?>?>() {}.type
        return gson.fromJson(json, listType)
    }

    @TypeConverter
    fun pollToJson(type: Poll?): String = gson.toJson(type)

    @TypeConverter
    fun jsonToPoll(json: String): Poll? =  gson.fromJson(json, Poll::class.java)

    @TypeConverter
    fun visibilityToJson(type: Status.Visibility?): String = gson.toJson(type)

    @TypeConverter
    fun jsonToVisibility(json: String): Status.Visibility? =  gson.fromJson(
        json,
        Status.Visibility::class.java
    )



}