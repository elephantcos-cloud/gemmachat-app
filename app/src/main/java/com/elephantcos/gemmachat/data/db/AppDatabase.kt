package com.elephantcos.gemmachat.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.elephantcos.gemmachat.data.dao.ConversationDao
import com.elephantcos.gemmachat.data.dao.MessageDao
import com.elephantcos.gemmachat.data.entity.Conversation
import com.elephantcos.gemmachat.data.entity.Message

@Database(entities = [Conversation::class, Message::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gemmachat_db"
                ).build().also { INSTANCE = it }
            }
    }
}
