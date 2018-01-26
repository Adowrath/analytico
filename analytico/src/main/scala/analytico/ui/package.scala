package analytico

import scalafx.Includes.handle
import scalafx.beans.property.BooleanProperty
import scalafx.beans.value.ObservableValue
import scalafx.scene.Node
import scalafx.scene.control.{ Button, CheckBox }
import scalafx.scene.layout.HBox
import java.lang.{ Boolean ⇒ JLBoolean }

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
  def button[R](name: String, disabled: ObservableValue[Boolean, JLBoolean] = null)(handler: ⇒ R): Button = {
    val b = new Button(name)
    if(disabled !== null)
      b.disable <== disabled
    b.onAction = handle(handler)
    b
  }

  /**
    * Constructs a simple [[HBox]] out of the given set of nodes.
    * Has no special property but to avoid anonymous subclasses.
    *
    * @param children the children of the to-be-constructed hbox
    *
    * @return a fresh hbox
    */
  def hbox(children: Node*): HBox = {
    val hb = new HBox()
    hb.children = children
    hb
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

  /**
    * Turns a time, represented by a BigInt, into a human readable time representation.
    *
    * @param seconds the seconds of the duration
    *
    * @return a string in the format `hours:minutes:seconds`
    *
    * @example
    * {{{
    * secondsToTime(  -6) == "-0:00:06"
    * secondsToTime(   6) == "0:00:06"
    * secondsToTime(  66) == "0:01:06"
    * secondsToTime( 666) == "0:11:06"
    * secondsToTime(6666) == "1:51:06"
    * }}}
    */
  def secondsToTime(seconds: BigInt): String = {
    val (prefix, actual) = if(seconds < 0) ("-", -seconds) else ("", seconds)
    val s = actual % 60
    val minutes = (actual - s) / 60
    val m = minutes % 60
    val hours = (minutes - m) / 60

    f"$prefix%s$hours%d:$m%02d:$s%02d"
  }
}
