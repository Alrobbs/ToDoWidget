package com.alexdo.todo

import android.app.AlertDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Widgets cannot draw dialogs. This is a fully transparent activity whose only
 * job is to show one, so from the home screen it looks like a popup over the widget.
 */
class ConfirmActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ID = "todo_id"
        const val EXTRA_TEXT = "todo_text"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val id = intent.getLongExtra(EXTRA_ID, -1L)
        val text = intent.getStringExtra(EXTRA_TEXT).orEmpty()
        if (id <= 0L) { finish(); return }

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Completed?")
            .setMessage(text)
            .setPositiveButton("Yes") { _, _ -> complete(id) }
            .setNegativeButton("Not yet") { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
    }

    private fun complete(id: Long) {
        lifecycleScope.launch {
            TodoDb.get(applicationContext).dao().setDone(id, true)
            TodoWidget().updateAll(applicationContext)
            finish()
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }
}
