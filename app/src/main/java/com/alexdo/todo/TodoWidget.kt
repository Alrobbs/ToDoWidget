package com.alexdo.todo

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import java.time.LocalDate

private val OFFSET = intPreferencesKey("day_offset")
val ID_PARAM = ActionParameters.Key<Long>(ConfirmActivity.EXTRA_ID)
val TEXT_PARAM = ActionParameters.Key<String>(ConfirmActivity.EXTRA_TEXT)

class TodoWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val offset = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)[OFFSET] ?: 0
        val date = LocalDate.now().plusDays(offset.toLong())
        val settings = Settings.read(context)
        val todos = TodoDb.get(context).dao().openFor(Fmt.iso(date))

        provideContent { Body(date, todos, settings) }
    }
}

@Composable
private fun Body(date: LocalDate, todos: List<Todo>, s: WidgetSettings) {
    val fg = if (s.darkText) Color(0xFF101010) else Color.White
    val fgProvider = ColorProvider(fg)
    val dim = ColorProvider(fg.copy(alpha = 0.55f))
    val shadow = ColorProvider(
        if (s.darkText) Color.White.copy(alpha = 0.70f)
        else Color.Black.copy(alpha = 0.85f)
    )
    val body = s.fontSizeSp.sp
    val header = (s.fontSizeSp + 2).sp

    val align = when (s.align) {
        Align.START -> TextAlign.Start
        Align.CENTER -> TextAlign.Center
        Align.END -> TextAlign.End
    }
    val rowAlign = when (s.align) {
        Align.START -> Alignment.Start
        Align.CENTER -> Alignment.CenterHorizontally
        Align.END -> Alignment.End
    }

    val root = GlanceModifier
        .fillMaxSize()
        .padding(horizontal = 10.dp, vertical = 8.dp)
        .let { if (s.backgroundDim > 0) it.background(ColorProvider(Color.Black.copy(alpha = s.backgroundDim / 100f))) else it }

    Column(modifier = root, horizontalAlignment = rowAlign) {

        // ---- Header: ‹  TO DO  date  › ----
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Arrow(
                glyph = "\u2039",
                color = dim,
                shadowColor = shadow,
                size = header,
                action = actionRunCallback<PreviousDateAction>()
            )
            Spacer(GlanceModifier.width(6.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                ShadowText(
                    text = "TO DO",
                    style = TextStyle(
                        color = fgProvider,
                        fontSize = header,
                        fontWeight = FontWeight.Medium,
                        textAlign = align
                    ),
                    modifier = GlanceModifier.fillMaxWidth()
                        .clickable(actionStartActivity<MainActivity>()),
                    shadowColor = shadow,
                    fillWidth = true
                )
                ShadowText(
                    text = Fmt.label(date),
                    style = TextStyle(color = dim, fontSize = (s.fontSizeSp - 2).sp, textAlign = align),
                    modifier = GlanceModifier.fillMaxWidth()
                        .clickable(actionRunCallback<TodayDateAction>()),
                    shadowColor = shadow,
                    fillWidth = true
                )
            }
            Spacer(GlanceModifier.width(6.dp))
            Arrow(
                glyph = "\u203A",
                color = dim,
                shadowColor = shadow,
                size = header,
                action = actionRunCallback<NextDateAction>()
            )
        }

        if (s.showDivider) {
            Spacer(GlanceModifier.height(4.dp))
            Spacer(
                GlanceModifier.fillMaxWidth().height(1.dp)
                    .background(ColorProvider(fg.copy(alpha = 0.35f)))
            )
        }
        Spacer(GlanceModifier.height(6.dp))

        // ---- Items ----
        if (todos.isEmpty()) {
            ShadowText(
                text = "Nothing here",
                style = TextStyle(color = dim, fontSize = body, textAlign = align),
                modifier = GlanceModifier.fillMaxWidth(),
                shadowColor = shadow,
                fillWidth = true
            )
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(todos.size) { i ->
                    val todo = todos[i]
                    val tap = if (s.confirmOnComplete) {
                        GlanceModifier.clickable(
                            actionStartActivity<ConfirmActivity>(
                                actionParametersOf(
                                    ID_PARAM to todo.id,
                                    TEXT_PARAM to todo.text
                                )
                            )
                        )
                    } else {
                        GlanceModifier.clickable(
                            actionRunCallback<CompleteAction>(actionParametersOf(ID_PARAM to todo.id))
                        )
                    }

                    Row(
                        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (s.align != Align.END) {
                            CheckBoxIcon(tap, fgProvider, s.fontSizeSp)
                            Spacer(GlanceModifier.width(8.dp))
                        }
                        ShadowText(
                            text = todo.text,
                            maxLines = 2,
                            style = TextStyle(color = fgProvider, fontSize = body, textAlign = align),
                            modifier = GlanceModifier.defaultWeight(),
                            shadowColor = shadow,
                            fillWidth = true
                        )
                        if (s.align == Align.END) {
                            Spacer(GlanceModifier.width(8.dp))
                            CheckBoxIcon(tap, fgProvider, s.fontSizeSp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckBoxIcon(tap: GlanceModifier, color: ColorProvider, fontSizeSp: Int) {
    val boxSize = (fontSizeSp + 4).dp
    Image(
        provider = ImageProvider(R.drawable.ic_box),
        contentDescription = "Mark complete",
        colorFilter = ColorFilter.tint(color),
        modifier = tap.size(boxSize)
    )
}

@Composable
private fun ShadowText(
    text: String,
    style: TextStyle,
    shadowColor: ColorProvider,
    modifier: GlanceModifier = GlanceModifier,
    maxLines: Int = Int.MAX_VALUE,
    fillWidth: Boolean = false
) {
    val textModifier = if (fillWidth) GlanceModifier.fillMaxWidth() else GlanceModifier
    Box(modifier = modifier) {
        Text(
            text = text,
            maxLines = maxLines,
            style = style.copy(color = shadowColor),
            modifier = textModifier.padding(start = 1.dp, top = 1.dp)
        )
        Text(
            text = text,
            maxLines = maxLines,
            style = style,
            modifier = textModifier
        )
    }
}

@Composable
private fun Arrow(
    glyph: String,
    color: ColorProvider,
    shadowColor: ColorProvider,
    size: androidx.compose.ui.unit.TextUnit,
    action: Action
) {
    ShadowText(
        text = glyph,
        style = TextStyle(color = color, fontSize = size),
        modifier = GlanceModifier
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .clickable(action),
        shadowColor = shadowColor
    )
}

private suspend fun shiftDate(context: Context, glanceId: GlanceId, delta: Int) {
    updateAppWidgetState(context, glanceId) { prefs ->
        prefs[OFFSET] = (prefs[OFFSET] ?: 0) + delta
    }
    TodoWidget().update(context, glanceId)
}

class PreviousDateAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) =
        shiftDate(context, glanceId, -1)
}

class NextDateAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) =
        shiftDate(context, glanceId, 1)
}

class TodayDateAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, glanceId) { prefs -> prefs[OFFSET] = 0 }
        TodoWidget().update(context, glanceId)
    }
}

/** Used when the "Completed?" confirmation is turned off. */
class CompleteAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[ID_PARAM] ?: return
        TodoDb.get(context).dao().setDone(id, true)
        TodoWidget().updateAll(context)
    }
}

class TodoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodoWidget()
}
