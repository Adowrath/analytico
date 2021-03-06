package analytico
package ui

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try
import scalafx.Includes._
import scalafx.application.Platform
import scalafx.beans.property.{ BooleanProperty, ObjectProperty, ReadOnlyIntegerProperty }
import scalafx.scene._
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control._
import scalafx.scene.layout._
import scalafx.stage._
import scalafxml.core.macros.sfxml

import org.scalactic.TypeCheckedTripleEquals._

import analytico.Main._
import analytico.ui.StatPane._

@sfxml
class MainController(val menu1: MenuItem, val tabPane: TabPane, val buttonSpace: VBox, loadedData: Map[String, StatPane]) {

  /* Constants */
  lazy val stage: Stage = tabPane.getScene.getWindow.asInstanceOf[javafx.stage.Stage]
  val errorShadow = "-fx-effect: dropshadow(two-pass-box, red, 10, 0.3, 0, 0)"
  val panes: mutable.Map[String, StatPane] = mutable.Map.empty
  val currentCancelable: ObjectProperty[Option[Cancelable]] = ObjectProperty(None)

  /* Initialization */
  for((name, pane) ← loadedData) {
    panes(name) = pane
    val tab = new Tab
    tab.text = name
    tabPane += tab
  }
  tabPane.tabs.headOption.foreach(fillTab(_))

  /* Properties */
  def currentTab: Tab = tabPane.selectionModel().getSelectedItem

  def currentIdx: ReadOnlyIntegerProperty = tabPane.selectionModel().selectedIndexProperty()

  val unsavedChanges: BooleanProperty = BooleanProperty(false)

  /* Utility methods */
  /**
    * Constructs a new Button that, when pressed, will run a given
    * authorization scheme while blocking other controls in the UI.
    *
    * @param buttonName the text to display on the button.
    * @param tab        the tab that will be filled with the results of the authorization.
    * @param handler    the authorization function.
    *
    * @return returns the new button
    */
  def apiButton(buttonName: String, tab: Tab)
               (handler: (String, BooleanProperty) ⇒ (Cancelable, Future[StatPane])): Button = button(buttonName) {
    toggleWaiting()
    val (cancelable, pane) = handler(tab.text(), unsavedChanges)
    currentCancelable() = Some(cancelable)
    pane map { pane ⇒
      panes(tab.text()) = pane
      toggleWaiting()
      Platform.runLater {
        buttonSpace.children = Nil
        pane.initialize(tab, Some(buttonSpace))
        buttonSpace.children.addAll(commonButtons(tab).map(_.delegate).asJava)
      }
    }
  }

  /* Actions */
  def newAccountPane(name: String): Unit = {
    panes(name) = NoStatsPane

    val tab = new Tab
    tab.text = name
    tabPane += tab
    tabPane.selectionModel().select(tab)
    unsavedChanges() = true
  }

  def toggleWaiting(): Unit =
    for(node ← buttonSpace.children :+ tabPane.delegate) {
      node.disable = !node.disable()
    }

  def stopWaiting(): Unit = {
    toggleWaiting()
    currentCancelable().foreach(_.cancel())
  }

  /* UI relevant. */
  def uninitializedButtons(tab: Tab): Seq[Node] = Seq(
    apiButton("Mit YouTube anmelden", tab) {
      YoutubeStatPane.apply
    }
  ) ++ commonButtons(tab)

  def commonButtons(tab: Tab): Seq[Node] = Seq(
    button("Umbenennen") {
      renameTab(tab)
    },
    button("Abbrechen", disabled = true) {
      stopWaiting()
    },
    hbox(
      button("<-", currentIdx === 0) {
        moveTab(tab, -1)
      },
      button("->", currentIdx === tabPane.tabs.size() - 1) {
        moveTab(tab, +1)
      }
    )
  )

  def moveTab(tab: Tab, offset: Int): Unit = {
    val oldIndex = tabPane.tabs.indexOf(tab)
    tabPane.tabs -= tab
    tabPane.tabs.add(oldIndex + offset, tab)
    tabPane.selectionModel().select(tab)
    unsavedChanges() = true
  }

  def fillTab(tab: Tab): Unit = {
    panes.get(tab.text()) match {
      case None | Some(NoStatsPane) ⇒
        buttonSpace.children = uninitializedButtons(tab)

      case Some(pane) ⇒
        buttonSpace.children = Nil
        pane.initialize(tab, Some(buttonSpace))
        buttonSpace.children ++= commonButtons(tab).map(_.delegate)
        ()
    }
  }

  /* Listeners */
  tabPane.selectionModel().selectedItemProperty().onChange((_, _, newTab) ⇒ fillTab(newTab))

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
        unsavedChanges() = true
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

  def save(): Try[Unit] =
    saveData(panes) map { _ ⇒
      unsavedChanges() = false
    }

  private[this] def finishAllThreads(): Unit = {
    currentCancelable().foreach(_.cancel())
  }

  Platform.runLater {
    stage.onCloseRequest = { event ⇒
      if(unsavedChanges()) {
        val Save = new ButtonType("Speichern")
        val Close = new ButtonType("Schliessen")

        val dialog = new Alert(AlertType.Warning)
        import dialog._
        initOwner(stage)
        title = "Sie haben ungesicherte Änderungen!"
        headerText = "Wollen Sie die Änderungen speichern?"
        buttonTypes = Seq(Save, Close, ButtonType.Cancel)

        val result = dialog.showAndWait()

        result match {
          case Some(`Save`) ⇒
            save().fold(throw _, _ ⇒ ())
            finishAllThreads()
          case Some(`Close`) ⇒
            ()
            finishAllThreads()
          case Some(_) | None ⇒
            event.consume()
        }
      }
    }
  }
}

