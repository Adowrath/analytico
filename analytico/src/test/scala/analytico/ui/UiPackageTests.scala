package analytico
package ui

import scalafx.beans.property.BooleanProperty

class UiPackageTests extends TestSpec {

  "The button constructor" should "correctly execute the handler" in {
    var clicked = false

    val b = button("myName") {
      clicked = true
    }

    clicked should === (false)
    b.fire()
    clicked should === (true)
  }

  it should "bind the disabled property correctly" in {
    var clicked = false
    val prop: BooleanProperty = BooleanProperty(true)

    val b = button("myName", prop) {
      clicked = true
    }

    b.fire()
    clicked should === (false)

    prop() = false
    b.fire()
    clicked should === (true)
  }

  "The checkBox constructor" should "correctly execute the handler" in {
    var clicked = false

    val cb = checkBox("myName") {
      clicked = true
    }

    clicked should === (false)
    cb.fire()
    clicked should === (true)
  }

  it should "bind the checked property correctly" in {
    val prop: BooleanProperty = BooleanProperty(true)

    val cb = checkBox("myName", prop)(())

    cb.selected() should === (true)
    prop() should === (true)

    cb.fire()
    cb.selected() should === (false)
    prop() should === (false)

    prop() = true
    cb.selected() should === (true)
    prop() should === (true)
  }
  
  "The seconds to time function" should "respect its examples" in {
    secondsToTime(  -6) should ===("-0:00:06")
    secondsToTime(   6) should ===("0:00:06")
    secondsToTime(  66) should ===("0:01:06")
    secondsToTime( 666) should ===("0:11:06")
    secondsToTime(6666) should ===("1:51:06")
  }
}
