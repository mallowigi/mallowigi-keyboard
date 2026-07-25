package com.mallowigi.keymap

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.ui.Messages
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class ContributeKeymapAction : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val activeKeymap = KeymapManager.getInstance().activeKeymap
    val baseKeymap = KeymapDiffService.findSourceKeymap(activeKeymap)

    if (baseKeymap == null) {
      Messages.showInfoMessage(
        project,
        "The active keymap is not based on any Mallowigi keymap. Please switch to a Mallowigi keymap before contributing.",
        "Mallowigi Keymap Contribution"
      )
      return
    }

    if (activeKeymap === baseKeymap) {
      Messages.showInfoMessage(
        project,
        "No changes detected in the active keymap compared to the base Mallowigi keymap.",
        "Mallowigi Keymap Contribution"
      )
      return
    }

    val diff = KeymapDiffService.computeDiff(activeKeymap, baseKeymap)
    if (diff.isEmpty()) {
      Messages.showInfoMessage(
        project,
        "No changes detected in the active keymap compared to the base Mallowigi keymap.",
        "Mallowigi Keymap Contribution"
      )
      return
    }

    val markdown = diff.toMarkdown()
    val result = Messages.showOkCancelDialog(
      project,
      "The following changes were detected:\n\n$markdown\nWould you like to contribute these changes to the repository?",
      "Contribute Keymap Changes",
      "Contribute",
      "Cancel",
      Messages.getQuestionIcon()
    )

    if (result == Messages.OK) {
      val title = "Keymap Contribution: ${baseKeymap.name}"
      val body = "I've made some changes to the ${baseKeymap.name} keymap that I'd like to contribute:\n\n$markdown"
      val encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8.toString())
      val encodedBody = URLEncoder.encode(body, StandardCharsets.UTF_8.toString())
      val url = "https://github.com/mallowigi/mallowigi-keyboard/issues/new?title=$encodedTitle&body=$encodedBody"

      BrowserUtil.browse(url)
    }
  }

  override fun update(e: AnActionEvent) {
    val activeKeymap = KeymapManager.getInstance().activeKeymap
    val baseKeymap = KeymapDiffService.findSourceKeymap(activeKeymap)
    e.presentation.isEnabledAndVisible = baseKeymap != null
  }
}
