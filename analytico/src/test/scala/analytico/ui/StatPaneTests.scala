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

  def youtubeStats: YoutubeStatPane = new YoutubeStatPane("credentialsName", "displayName", "channelId", None, BooleanProperty(false))

  def jsonYoutubeStats: Json = Json.obj(
    "credentials" → "credentialsName".asJson,
    "displayName" → "displayName".asJson,
    "channelId" → "channelId".asJson,
    "live-stats" → true.asJson,
    "video-stats" → false.asJson,
    "combined-stats" → false.asJson
  )

  "A YoutubeStatPane" should behave like circeTests(youtubeStats, jsonYoutubeStats, "YoutubeStatPane")

  it should "invalidate correctly" in {
    val ys = new YoutubeStatPane("credentialsName", "displayName", "channelId", Some(new MockAnalytics), BooleanProperty(false))
    val prop = ys.unsavedMarker

    ys.initialize(new Tab, None)

    /* Zu Beginn: Nichts geändert. */
    prop() shouldBe false
    ys.isValid shouldBe true

    ys.combinedStats() = true

    prop() shouldBe true
    ys.isValid shouldBe false
  }

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
