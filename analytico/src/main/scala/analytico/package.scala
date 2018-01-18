/**
  * Analytico is a library/program for collecting and
  * exporting analytics data and exporting it to Excel files.
  */
package object analytico {

  /** Eine kleine Fiktion, um einen ===-Operator anzubieten. */
  @SuppressWarnings(Array("org.wartremover.warts.Equals"))
  implicit class IdEquals[A](val a: A) extends AnyVal {
    @inline
    def ===(other: A): Boolean = a == other
    @inline
    def =/=(other: A): Boolean = a != other
  }

}
