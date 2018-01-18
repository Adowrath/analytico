package analytico
package ui

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.beans.property.ObjectProperty
import scalafx.scene._
import scalafx.scene.control._
import scalafx.scene.layout._
import scalafx.stage._
import scalafxml.core.macros.sfxml

import analytico.ui.StatPane._

@sfxml
class MainController(val menu1: MenuItem, val tabPane: TabPane, val buttonSpace: VBox) {

  /* Constants */
  lazy val stage: Stage = tabPane.getScene.getWindow.asInstanceOf[javafx.stage.Stage]
  val errorShadow = "-fx-effect: dropshadow(two-pass-box, red, 10, 0.3, 0, 0)"
  val panes: mutable.Map[String, StatPane] = mutable.Map.empty
  val currentCancelable: ObjectProperty[Option[Cancelable]] = ObjectProperty(None)

  /* Properties */
  def currentTab: Tab = tabPane.selectionModel().getSelectedItem
  def nodesToDisable: Seq[Node] =
    buttonSpace.children.map(n ⇒ n: Node).dropRight(1) :+ tabPane


  def disable(waiting: Boolean): Unit = {
    nodesToDisable.foreach(_.disable = waiting)
  }

  def stopWaiting(): Unit = {
    disable(waiting = false)
    currentCancelable().foreach(_.cancel())
  }

  def apiButton(buttonName: String, tab: Tab)(handler: String ⇒ (Cancelable, Future[StatPane])): Button = button(buttonName) {
    disable(waiting = true)
    val (cancelable, pane) = handler(tab.text())
    currentCancelable() = Some(cancelable)
    pane map { pane ⇒
      panes(tab.text()) = pane
      disable(waiting = false)
      Platform.runLater {
        buttonSpace.children = Nil
        pane.initialize(tab, Some(buttonSpace))
        buttonSpace.children.addAll(commonButtons(tab).map(_.delegate).asJava)
      }
    }
  }

  def getApiButtons(tab: Tab): Seq[Button] = Seq(
    apiButton("Mit YouTube anmelden", tab) {
      YoutubeStatPane.apply
    }
    // TODO: Andere APIs
  ) ++ commonButtons(tab)

  def commonButtons(tab: Tab): Seq[Button] = Seq(
    button("Umbenennen") {
      renameTab(tab)
    },
    button("Abbrechen") {
      stopWaiting()
    }
  )

  def renameTab(tab: Tab): Unit = {
    val oldName = tab.text()

    val dialog = new TextInputDialog(oldName)
    import dialog._
    initOwner(stage)
    title = "Datenblatt umbenennen"
    headerText = "Wie soll das Datenblatt neu heissen?"
    locally {
      val okButton = dialog.dialogPane().lookupButton(ButtonType.OK)
      okButton.disable = true
      editor.text.onChange { (_, _, newText) ⇒
        val trimmed = newText.trim
        okButton.disable = trimmed.isEmpty
        if(trimmed === oldName) {
          okButton.disable = true
          headerText = "Wie soll das Datenblatt neu heissen?"
          editor.style = ""
        } else if(panes.contains(trimmed)) {
          okButton.disable = true
          headerText = "Der Name ist bereits vorhanden!"
          editor.style = errorShadow
        } else {
          editor.style = ""
          headerText = "Wie soll das Datenblatt neu heissen?"
        }
      }
    }
    onCloseRequest = handle {
      for(text <- Option(dialog.result())) {
        val newName = text.trim
        val pane = panes(oldName)
        panes -= oldName
        tab.text() = newName
        panes(newName) = pane
      }
    }
    dialog.show()
  }

  def addSheet(): Unit = {
    val dialog = new TextInputDialog
    import dialog._
    initOwner(stage)
    title = "Neues Datenblatt"
    headerText = "Bitte geben Sie einen Titel an."
    locally {
      val okButton = dialog.dialogPane().lookupButton(ButtonType.OK)
      okButton.disable = true
      editor.text.onChange { (_, _, newText) ⇒
        okButton.disable = newText.trim.isEmpty
        if(panes.contains(newText.trim)) {
          okButton.disable = true
          headerText = "Der Name ist bereits vorhanden!"
          editor.style = errorShadow
        } else {
          editor.style = ""
          headerText = "Bitte geben Sie einen Titel an."
        }
      }
    }
    onCloseRequest = handle {
      for(title <- Option(dialog.result())) newAccountPane(title.trim)
    }
    dialog.show()
  }

  def newAccountPane(name: String): Unit = {
    panes(name) = NoStatsPane

    val tab = new Tab
    tab.text = name
    tabPane += tab
    tabPane.selectionModel().select(tab)
  }

  tabPane.selectionModel().selectedItemProperty().onChange { (_, _, newTab) ⇒
    panes.get(newTab.getText) match {
      case None | Some(NoStatsPane) ⇒
        buttonSpace.children = getApiButtons(newTab)

      case Some(pane) ⇒
        buttonSpace.children = Nil
        pane.initialize(newTab, Some(buttonSpace))
        buttonSpace.children.addAll(commonButtons(newTab).map(_.delegate).asJava)
    }
  }

  def clicked(): Unit = {

  }
}
