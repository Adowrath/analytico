package analytico

import scalafx.Includes.handle
import scalafx.beans.property.BooleanProperty
import scalafx.scene.control.Button

package object ui {

  /**
    * Constructs a simple button with a custom handler.
    *
    * @param name    the text that will be displayed on the button.
    * @param handler a handler not requiring the event instance.
    * @tparam R a return type, used to avoid 'discarded non-Unit value' warnings. Unused.
    *
    * @return a button with the specified handler
    */
  def button[R](name: String)(handler: ⇒ R): Button = {
    val b = new Button(name)
    b.onAction = handle(handler)
    b
  }

  /**
    * Constructs a simple button with a disabled-property binding and a custom handler.
    *
    * @param name    the text that will be displayed on the button.
    *                @param disabledProp a disabling-property.
    * @param handler a handler not requiring the event instance.
    * @tparam R a return type, used to avoid 'discarded non-Unit value' warnings. Unused.
    *
    * @return a button with the specified handler
    */
  def button[R](name: String, disabledProp: BooleanProperty)(handler: ⇒ R): Button = {
    val b = new Button(name)
    b.disable <== disabledProp
    b.onAction = handle(handler)
    b
  }
}
