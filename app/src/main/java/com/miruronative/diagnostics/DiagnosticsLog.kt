package com.miruronative.diagnostics

import android.util.Log

object DiagnosticsLog {
    fun event(message: String) { Log.d("Diag", message) }
    fun throwable(message: String, throwable: Throwable) { Log.e("Diag", message, throwable) }
}
