package org.jetbrains.plugins.scala.codeInsight.implicits

import com.intellij.openapi.actionSystem.{AnActionEvent, ToggleAction}

class ToggleImplicitHintsAction extends ToggleAction {
  override def isSelected(event: AnActionEvent): Boolean =
    ImplicitHints.enabled

  override def setSelected(event: AnActionEvent, value: Boolean): Unit = {
    ImplicitHints.enabled = value
  }
}
