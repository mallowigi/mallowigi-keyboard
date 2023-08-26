package com.mallowigi.keymap

import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.project.Project

val IGNORED_PREFIXES = arrayOf("Enter", "Exit", "Toggle")
const val DEFAULT_CHAR = ' '

fun shouldExitOnProjectStatus(project: Project): Boolean =
  project.isDisposed || project.isDefault || !project.isInitialized

fun trimIgnoredPrefixes(text: String): String = IGNORED_PREFIXES.fold(text) { acc, ignoredPrefix ->
  acc.replace(ignoredPrefix, "")
}.trim()

fun extractUppercaseChars(text: String): Pair<Char, Char> {
  val upperCaseArray = text.filter { it.isUpperCase() }
    .take(2)
    .toCharArray()
  val firstUppercase = upperCaseArray.getOrElse(0) { DEFAULT_CHAR }
  val secondUppercase = upperCaseArray.getOrElse(1) { DEFAULT_CHAR }

  return firstUppercase to secondUppercase
}

fun hasShortcut(shortcut: KeyboardShortcut): Boolean =
  KeymapManager.getInstance().activeKeymap.getActionIds(shortcut).isNotEmpty()
