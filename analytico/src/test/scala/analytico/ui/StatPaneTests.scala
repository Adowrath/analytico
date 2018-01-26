package analytico
package ui

import scalafx.beans.property.BooleanProperty
import scalafx.scene.control.Tab
import scalafx.scene.layout.Pane

import analytico.ui.StatPane.{ NoStatsPane, YoutubeStatPane }
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._

class StatPaneTests extends TestSpec with CirceSpec[StatPane] with YTApiMocks {

  val youtubeStats: YoutubeStatPane =
    new YoutubeStatPane("credentialsName", "displayName", "channelId", None, BooleanProperty(false))

  val jsonYoutubeStats: Json = Json.obj(
    "credentials" → "credentialsName".asJson,
    "displayName" → "displayName".asJson,
    "channelId" → "channelId".asJson,
    "live-stats" → true.asJson,
    "video-stats" → false.asJson,
    "combined-stats" → false.asJson
  )

  "A YoutubeStatPane" should behave like circeTests(youtubeStats, jsonYoutubeStats, "YoutubeStatPane")

  def invalidationCheck(value: Boolean, name: String, invalidator: YoutubeStatPane ⇒ BooleanProperty): Unit = {
    it should s"invalidate $name correctly" in {
      val analytics = new MockAnalytics(
        List("1900-01-01", "LIVE", (1: BigDecimal).bigDecimal, (5: BigDecimal).bigDecimal),
        List("1900-01-01", "ON_DEMAND", (99: BigDecimal).bigDecimal, (995: BigDecimal).bigDecimal)
      )
      val ys = new YoutubeStatPane("credentialsName", "displayName", "channelId", Some(analytics), BooleanProperty(false))
      val prop = ys.unsavedMarker

      ys.initialize(new Tab, None)

      /* Zu Beginn: Nichts geändert. */
      prop() shouldBe false
      ys.isValid shouldBe true

      invalidator(ys)() = value

      prop() shouldBe true
      ys.isValid shouldBe false
    }
  }

  it should behave like invalidationCheck(value = false, "live", _.liveStats)
  it should behave like invalidationCheck(value = true, "video", _.videoStats)
  it should behave like invalidationCheck(value = true, "combined", _.combinedStats)

  "A NoStatPane" should behave like circeTests(NoStatsPane, Json.obj(), "NoStatsPane")

  it should "treat initialize as a no-op" in {
    val tab1 = new Tab()
    val tab2 = new Tab()
    val pane = new Pane()

    NoStatsPane.initialize(tab2, Some(pane))

    pane.children.size() should ===(0)
    tab2.text() should ===(tab1.text())
    tab2.content() should ===(tab1.content())
  }
}
