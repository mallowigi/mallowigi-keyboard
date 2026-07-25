package com.mallowigi.keymap

import com.intellij.openapi.actionSystem.Shortcut
import com.intellij.openapi.keymap.Keymap
import com.intellij.openapi.keymap.KeymapUtil

data class KeymapDiff(
  val added: Map<String, List<Shortcut>>,
  val changed: Map<String, Pair<List<Shortcut>, List<Shortcut>>>,
  val removed: List<String>
) {
  fun isEmpty(): Boolean = added.isEmpty() && changed.isEmpty() && removed.isEmpty()

  fun toMarkdown(): String {
    val sb = StringBuilder()
    if (added.isNotEmpty()) {
      sb.append("### Added Shortcuts\n")
      added.forEach { (actionId, shortcuts) ->
        sb.append("- **$actionId**: ${shortcuts.joinToString { KeymapUtil.getShortcutText(it) }}\n")
      }
      sb.append("\n")
    }

    if (changed.isNotEmpty()) {
      sb.append("### Changed Shortcuts\n")
      changed.forEach { (actionId, pair) ->
        val (old, new) = pair
        sb.append(
          "- **$actionId**: ${old.joinToString { KeymapUtil.getShortcutText(it) }} -> ${
            new.joinToString {
              KeymapUtil.getShortcutText(
                it
              )
            }
          }\n"
        )
      }
      sb.append("\n")
    }

    if (removed.isNotEmpty()) {
      sb.append("### Removed Shortcuts\n")
      removed.forEach { actionId ->
        sb.append("- **$actionId**\n")
      }
      sb.append("\n")
    }

    return sb.toString()
  }
}

object KeymapDiffService {
  val MALLOWIGI_KEYMAPS: Set<String> = setOf("Mallowigi OSX", "Mallowigi Linux", "Mallowigi Win")

  fun findSourceKeymap(activeKeymap: Keymap): Keymap? {
    var current: Keymap? = activeKeymap
    while (current != null) {
      if (MALLOWIGI_KEYMAPS.contains(current.name)) {
        return current
      }
      current = current.parent
    }
    return null
  }

  fun computeDiff(activeKeymap: Keymap, baseKeymap: Keymap): KeymapDiff {
    val added = mutableMapOf<String, List<Shortcut>>()
    val changed = mutableMapOf<String, Pair<List<Shortcut>, List<Shortcut>>>()
    val removed = mutableListOf<String>()

    val allActionIds = activeKeymap.actionIds.toSet() + baseKeymap.actionIds.toSet()

    for (actionId in allActionIds) {
      val activeShortcuts = activeKeymap.getShortcuts(actionId).toList()
      val baseShortcuts = baseKeymap.getShortcuts(actionId).toList()

      when {
        baseShortcuts.isEmpty() && activeShortcuts.isNotEmpty() -> added[actionId] = activeShortcuts
        baseShortcuts.isNotEmpty() && activeShortcuts.isEmpty() -> removed.add(actionId)
        baseShortcuts != activeShortcuts                        -> changed[actionId] = baseShortcuts to activeShortcuts
      }
    }

    return KeymapDiff(added, changed, removed)
  }
}
