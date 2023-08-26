package com.mallowigi.keymap

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.AnActionResult
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil

class ShortcutDetective(private val project: Project) : AnActionListener {
  private var notification: Notification? = null

  override fun afterActionPerformed(action: AnAction, event: AnActionEvent, result: AnActionResult) {
    if (shouldExitOnProjectStatus(project)
      || shouldExitOnEventPlace(event)
      || shouldExitOnActionShortcut(event, action)
    ) return

    val actionId = event.actionManager.getId(action)
    val text = event.presentation.text
    if (StringUtil.isEmpty(text) || actionId == null || !text.startsWith(TOGGLE_ACTION)) return

    notifyUser(text)
  }

  private fun shouldExitOnEventPlace(event: AnActionEvent): Boolean = event.place !== "keyboard shortcut"

  private fun shouldExitOnActionShortcut(event: AnActionEvent, action: AnAction): Boolean {
    val actionId = event.actionManager.getId(action)
    return actionId == null || KeymapManager.getInstance().activeKeymap.getShortcuts(actionId).isEmpty()
  }

  private fun notifyUser(text: String) {
    val notification = createNotification(text)
    notification.notify(project)

    this.notification = notification
  }

  private fun createNotification(text: String): Notification {
    val (firstChar, secondChar) = extractUppercaseChars(text)
    val decoratedText = text
      .replace(firstChar.toString(), "<b>$firstChar</b>")
      .replace(secondChar.toString(), "<b>$secondChar</b>")

    return Notification(
      NOTIFICATION_ID,
      "Shortcut identified!",
      "Shortcut '${decoratedText}' has been pressed",
      NotificationType.INFORMATION
    )
  }

  companion object {
    const val NOTIFICATION_ID = "Mallowigi Shortcut Detective"
    const val TOGGLE_ACTION = "Toggle"
  }
}
