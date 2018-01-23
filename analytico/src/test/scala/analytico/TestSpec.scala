package analytico

import javafx.embed.swing.JFXPanel

import org.scalatest.{ EitherValues, FlatSpec, Matchers, PrivateMethodTester }

/**
  * Super-Trait für die Tests in Analytico.
  */
trait TestSpec extends FlatSpec with Matchers with EitherValues with PrivateMethodTester {
  /** Wärmt den JavaFX-Stack auf. */
  val _ = new JFXPanel()
}
