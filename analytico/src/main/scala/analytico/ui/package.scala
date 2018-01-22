package analytico

import scalafx.Includes.handle
import scalafx.beans.property.BooleanProperty
import scalafx.scene.control.{ Button, CheckBox }

import org.scalactic.TypeCheckedTripleEquals._

package object ui {
  /**
    * Constructs a simple button with a custom handler, and, optionally,
    * a `disabled` property that will be bound to the button (`button <== disable`).
    *
    * @param name     the text that will be displayed on the button.
    * @param disabled this property will be bound to the button.
    *                 Updates in its values will be reflected in the button, not the other way around.
    * @param handler  a handler not requiring the event instance.
    * @tparam R a return type, used to avoid 'discarded non-Unit value' warnings.
    *           The resulting actual value is unused.
    *
    * @return a button with the specified handler
    */
  def button[R](name: String, disabled: BooleanProperty = null)(handler: ⇒ R): Button = {
    val b = new Button(name)
    if(disabled !== null)
      b.disable <== disabled
    b.onAction = handle(handler)
    b
  }

  /**
    * Constructs a simple checkbox with a custom handler, and, optionally,
    * a `checked` property that will be bound to the checkbox (`button <==> checked`).
    *
    * @param name    the text that will be displayed on the checkbox.
    * @param checked this property will be bound to the checkbox.
    *                Updates in both will reflect in the other.
    * @param handler a handler not requiring the event instance.
    * @tparam R a return type, used to avoid 'discarded non-Unit value' warnings.
    *           The resulting actual value is unused.
    *
    * @return a button with the specified handler
    */
  def checkBox[R](name: String, checked: BooleanProperty = null)(handler: ⇒ R): CheckBox = {
    val cb = new CheckBox(name)
    if(checked !== null)
      cb.selected <==> checked
    cb.onAction = handle(handler)
    cb
  }
}
