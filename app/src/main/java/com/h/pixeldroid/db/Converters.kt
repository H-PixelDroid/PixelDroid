package com.h.pixeldroid.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.h.pixeldroid.objects.*
import java.util.*

class Converters {
    @TypeConverter
    fun listToJson(list: List<String>): String = Gson().toJson(list)

     @TypeConverter
     fun jsonToList(json: String): List<String> =
         Gson().fromJson(json, Array<String>::class.java).toList()

    @TypeConverter
    fun dateToJson(date: Date): String = Gson().toJson(date)

    @TypeConverter
    fun jsonToDate(json: String): Date = Gson().fromJson(json, Date::class.java)

    @TypeConverter
    fun accountToJson(account: Account): String = Gson().toJson(account)

    @TypeConverter
    fun jsonToAccount(json: String): Account =  Gson().fromJson(json, Account::class.java)

    @TypeConverter
    fun statusToJson(status: Status?): String = Gson().toJson(status)

    @TypeConverter
    fun jsonToStatus(json: String): Status? =  Gson().fromJson(json, Status::class.java)

    @TypeConverter
    fun notificationTypeToJson(type: Notification.NotificationType?): String = Gson().toJson(type)

    @TypeConverter
    fun jsonToNotificationType(json: String): Notification.NotificationType? =  Gson().fromJson(
        json,
        Notification.NotificationType::class.java
    )

    @TypeConverter
    fun applicationToJson(type: Application?): String = Gson().toJson(type)

    @TypeConverter
    fun jsonToApplication(json: String): Application? =  Gson().fromJson(
        json,
        Application::class.java
    )

    @TypeConverter
    fun cardToJson(type: Card?): String = Gson().toJson(type)

    @TypeConverter
    fun jsonToCard(json: String): Card? =  Gson().fromJson(json, Card::class.java)

    @TypeConverter
    fun attachmentToJson(type: Attachment?): String = Gson().toJson(type)

    @TypeConverter
    fun jsonToAttachment(json: String): Attachment? =  Gson().fromJson(json, Attachment::class.java)

    @TypeConverter
    fun attachmentListToJson(type: List<Attachment>?): String {
        val listType = object : TypeToken<List<Attachment?>?>() {}.type
        return Gson().toJson(type, listType)
    }

    @TypeConverter
    fun jsonToAttachmentList(json: String): List<Attachment>? {
        val listType = object : TypeToken<List<Attachment?>?>() {}.type
        return Gson().fromJson(json, listType)
    }

    @TypeConverter
    fun mentionListToJson(type: List<Mention>?): String {
        val listType = object : TypeToken<List<Mention?>?>() {}.type
        return Gson().toJson(type, listType)
    }

    @TypeConverter
    fun jsonToMentionList(json: String): List<Mention>? {
        val listType = object : TypeToken<List<Mention?>?>() {}.type
        return Gson().fromJson(json, listType)
    }

    @TypeConverter
    fun emojiListToJson(type: List<Emoji>?): String {
        val listType = object : TypeToken<List<Emoji?>?>() {}.type
        return Gson().toJson(type, listType)
    }

    @TypeConverter
    fun jsonToEmojiList(json: String): List<Emoji>? {
        val listType = object : TypeToken<List<Emoji?>?>() {}.type
        return Gson().fromJson(json, listType)
    }

    @TypeConverter
    fun tagListToJson(type: List<Tag>?): String {
        val listType = object : TypeToken<List<Tag?>?>() {}.type
        return Gson().toJson(type, listType)
    }

    @TypeConverter
    fun jsonToTagList(json: String): List<Tag>? {
        val listType = object : TypeToken<List<Tag?>?>() {}.type
        return Gson().fromJson(json, listType)
    }

    @TypeConverter
    fun pollToJson(type: Poll?): String = Gson().toJson(type)

    @TypeConverter
    fun jsonToPoll(json: String): Poll? =  Gson().fromJson(json, Poll::class.java)

    @TypeConverter
    fun visibilityToJson(type: Status.Visibility?): String = Gson().toJson(type)

    @TypeConverter
    fun jsonToVisibility(json: String): Status.Visibility? =  Gson().fromJson(
        json,
        Status.Visibility::class.java
    )



}