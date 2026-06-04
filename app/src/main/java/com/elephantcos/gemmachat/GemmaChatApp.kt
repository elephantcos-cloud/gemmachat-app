package com.elephantcos.gemmachat

import android.app.Application
import com.elephantcos.gemmachat.data.db.AppDatabase

class GemmaChatApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
}
