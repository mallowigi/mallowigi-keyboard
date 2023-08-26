package com.mallowigi.keymap

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.AnActionResult
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import javax.swing.KeyStroke

class ShortcutNotifier(private val project: Project) : AnActionListener {
  private var notification: Notification? = null

  override fun afterActionPerformed(action: AnAction, event: AnActionEvent, result: AnActionResult) {
    if (shouldExitOnProjectStatus(project)
      || shouldExitOnEventPlace(event)
      || shouldExitOnActionShortcut(event, action)
    ) return

    val actionId = event.actionManager.getId(action)
    var text = event.presentation.text
    var template = action.templatePresentation.text
    if (StringUtil.isEmpty(text) || StringUtil.isEmpty(template) || actionId == null) return

    text = trimIgnoredPrefixes(text)
    template = trimIgnoredPrefixes(template)
    if (text.lowercase() != template.lowercase()) return

    val newShortcut = createShortcutFromText(text)
    if (hasShortcut(newShortcut)) return

    notifyUser(actionId, text, newShortcut)
  }

  private fun shouldExitOnProjectStatus(project: Project): Boolean =
    project.isDisposed || project.isDefault || !project.isInitialized

  private fun shouldExitOnEventPlace(event: AnActionEvent): Boolean = event.place === "keyboard shortcut"

  private fun shouldExitOnActionShortcut(event: AnActionEvent, action: AnAction): Boolean {
    val actionId = event.actionManager.getId(action)
    return actionId == null || KeymapManager.getInstance().activeKeymap.getShortcuts(actionId).isNotEmpty()
  }

  private fun trimIgnoredPrefixes(text: String): String = IGNORED_PREFIXES.fold(text) { acc, ignoredPrefix ->
    acc.replace(ignoredPrefix, "")
  }.trim()

  private fun extractUppercaseChars(text: String) = text.filter { it.isUpperCase() }
    .take(2)
    .toCharArray()
    .let { (firstUppercase, secondUppercase) -> firstUppercase to secondUppercase }

  private fun hasShortcut(shortcut: KeyboardShortcut): Boolean =
    KeymapManager.getInstance().activeKeymap.getActionIds(shortcut).isNotEmpty()

  private fun createShortcutFromText(text: String): KeyboardShortcut {
    val (firstChar, secondChar) = extractUppercaseChars(text)
    return KeyboardShortcut(
      KeyStroke.getKeyStroke("alt $firstChar"),
      KeyStroke.getKeyStroke("alt $secondChar")
    )
  }

  private fun notifyUser(actionId: String, text: String, newShortcut: KeyboardShortcut) {
    val notification = createNotification(text, newShortcut)
    notification.addAction(createAssignAction(actionId, newShortcut))
    notification.notify(project)

    this.notification = notification
  }

  private fun createNotification(text: String, newShortcut: KeyboardShortcut): Notification {
    val (firstChar, secondChar) = extractUppercaseChars(text)
    val decoratedText = text
      .replace(firstChar.toString(), "<b>$firstChar</b>")
      .replace(secondChar.toString(), "<b>$secondChar</b>")
    val shortcutText = KeymapUtil.getShortcutText(newShortcut)

    return Notification(
      NOTIFICATION_ID,
      "A Shortcut Appears!",
      "<html><body>You may assign the ($shortcutText) sequence for the action '${decoratedText}'",
      NotificationType.INFORMATION
    )
  }

  private fun createAssignAction(actionId: String, newShortcut: KeyboardShortcut): AnAction =
    object : AnAction("Assign") {
      override fun actionPerformed(e: AnActionEvent) {
        KeymapManager.getInstance().activeKeymap.addShortcut(actionId, newShortcut)
        notification?.expire()
        notification = null
      }
    }

  companion object {
    val IGNORED_PREFIXES = arrayOf("Enter", "Exit", "Toggle")
    const val NOTIFICATION_ID = "A Shortcut Appears!!!"
  }
}
