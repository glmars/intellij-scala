package org.jetbrains.bsp.project

import java.util

import com.intellij.openapi.components._
import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings
import com.intellij.openapi.externalSystem.service.settings.AbstractExternalProjectSettingsControl
import com.intellij.openapi.externalSystem.settings._
import com.intellij.openapi.externalSystem.util.ExternalSystemUiUtil._
import com.intellij.openapi.externalSystem.util.{ExternalSystemSettingsControl, PaintAwarePanel}
import com.intellij.openapi.project.Project
import com.intellij.uiDesigner.core.{GridConstraints, GridLayoutManager, Spacer}
import com.intellij.util.containers.ContainerUtilRt
import com.intellij.util.messages.Topic
import javax.swing.{JCheckBox, JPanel}
import org.jetbrains.bsp._

import scala.beans.BeanProperty

class BspProjectSettings extends ExternalProjectSettings {

  @BeanProperty
  var buildOnSave = false

  override def clone(): BspProjectSettings = {
    val result = new BspProjectSettings
    copyTo(result)
    result.buildOnSave = buildOnSave
    result
  }
}

// TODO the hell is up with this setting duplication
class BspProjectSettingsControl(settings: BspProjectSettings)
  extends AbstractExternalProjectSettingsControl[BspProjectSettings](null, settings, null) {

  @BeanProperty
  var buildOnSave = false

  private val buildOnSaveCheckBox = new JCheckBox("build automatically on file save")

  override def fillExtraControls(content: PaintAwarePanel, indentLevel: Int): Unit = {
    val fillLineConstraints = getFillLineConstraints(1)
    content.add(buildOnSaveCheckBox, fillLineConstraints)
  }

  override def isExtraSettingModified: Boolean = {
    val initial = getInitialSettings
    buildOnSaveCheckBox.isSelected != initial.buildOnSave
  }

  override def resetExtraSettings(isDefaultModuleCreation: Boolean): Unit = {
    val initial = getInitialSettings
    buildOnSaveCheckBox.setSelected(initial.buildOnSave)
  }

  override def applyExtraSettings(settings: BspProjectSettings): Unit = {
    settings.buildOnSave = buildOnSaveCheckBox.isSelected
  }

  override def validate(settings: BspProjectSettings): Boolean = true

  override def updateInitialExtraSettings(): Unit = {
    applyExtraSettings(getInitialSettings)
  }

}


/** A dummy to satisfy interface constraints of ExternalSystem */
trait BspProjectSettingsListener extends ExternalSystemSettingsListener[BspProjectSettings]

class BspProjectSettingsListenerAdapter(listener: ExternalSystemSettingsListener[BspProjectSettings])
  extends DelegatingExternalSystemSettingsListener[BspProjectSettings](listener) with BspProjectSettingsListener


object BspTopic extends Topic[BspProjectSettingsListener]("bsp-specific settings", classOf[BspProjectSettingsListener])

@State(
  name = "BspSettings",
  storages = Array(new Storage("bsp.xml"))
)
class BspSystemSettings(project: Project)
  extends AbstractExternalSystemSettings[BspSystemSettings, BspProjectSettings, BspProjectSettingsListener](BspTopic, project)
    with PersistentStateComponent[BspSystemSettingsState]
{
  override def subscribe(listener: ExternalSystemSettingsListener[BspProjectSettings]): Unit = {
    val adapter = new BspProjectSettingsListenerAdapter(listener)
    getProject.getMessageBus.connect(getProject).subscribe(BspTopic, adapter)
  }

  override def copyExtraSettingsFrom(settings: BspSystemSettings): Unit = {}

  override def checkSettings(old: BspProjectSettings, current: BspProjectSettings): Unit = {}

  override def getState: BspSystemSettingsState = {
    val state = new BspSystemSettingsState
    fillState(state)
    state
  }

  override def loadState(state: BspSystemSettingsState): Unit =
    super[AbstractExternalSystemSettings].loadState(state)
}

object BspSystemSettings {
  def getInstance(project: Project): BspSystemSettings = ServiceManager.getService(project, classOf[BspSystemSettings])
}

class BspSystemSettingsState extends AbstractExternalSystemSettings.State[BspProjectSettings] {
  private val projectSettings = ContainerUtilRt.newTreeSet[BspProjectSettings]()

  override def getLinkedExternalProjectsSettings: util.Set[BspProjectSettings] = projectSettings
  override def setLinkedExternalProjectsSettings(settings: util.Set[BspProjectSettings]): Unit =
    projectSettings.addAll(settings)
}

@State(
  name = "BspLocalSettings",
  storages = Array(new Storage(StoragePathMacros.WORKSPACE_FILE))
)
class BspLocalSettings(project: Project)
  extends AbstractExternalSystemLocalSettings[BspLocalSettingsState](bsp.ProjectSystemId, project)
    with PersistentStateComponent[BspLocalSettingsState] {

  override def loadState(state: BspLocalSettingsState): Unit =
    super[AbstractExternalSystemLocalSettings].loadState(state)
}

object BspLocalSettings {
  def getInstance(project: Project): BspLocalSettings = ServiceManager.getService(project, classOf[BspLocalSettings])
}

class BspLocalSettingsState extends AbstractExternalSystemLocalSettings.State

class BspExecutionSettings extends ExternalSystemExecutionSettings

object BspExecutionSettings {

  def executionSettingsFor(project: Project, path: String): BspExecutionSettings =
    new BspExecutionSettings
}

class BspSystemSettingsControl(settings: BspSystemSettings) extends ExternalSystemSettingsControl[BspSystemSettings] {

  private val panel = new JPanel()

  override def fillUi(canvas: PaintAwarePanel, indentLevel: Int): Unit = {


    panel.setLayout(new GridLayoutManager(2, 1))
    val spacer = new Spacer
    panel.add(spacer, new GridConstraints())

    canvas.add(panel)
  }

  override def showUi(show: Boolean): Unit =
    panel.setVisible(show)

  override def reset(): Unit = {}
  override def isModified: Boolean = false
  override def apply(settings: BspSystemSettings): Unit = {}
  override def validate(settings: BspSystemSettings): Boolean = true
  override def disposeUIResources(): Unit = {}
}
