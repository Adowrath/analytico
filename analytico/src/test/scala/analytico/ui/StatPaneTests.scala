package analytico
package ui

import scalafx.beans.property.BooleanProperty

import org.scalactic.CanEqual
import org.scalatest._

import analytico.ui.StatPane.YoutubeStatPane
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._

class StatPaneTests extends FlatSpec with Matchers with EitherValues with PrivateMethodTester {

  private val youtubeStatsCanEqual: YoutubeStatPane CanEqual YoutubeStatPane = { (left, right) ⇒
    left.credentialsName === right.credentialsName &&
      left.displayName === right.displayName &&
      left.channelId === right.channelId
  }

  val youtubeStats: YoutubeStatPane = new YoutubeStatPane("credentialsName", "displayName", "channelId", None, BooleanProperty(false))
  val jsonYoutubeStats: Json = Json.obj(
    "credentials" → "credentialsName".asJson,
    "displayName" → "displayName".asJson,
    "channelId" → "channelId".asJson,
    "live-stats" → true.asJson,
    "video-stats" → false.asJson,
    "combined-stats" → false.asJson
  )

  "A YoutubeStatPane" should "serialize to Json correctly" in {
    youtubeStats.asJson should ===(jsonYoutubeStats)
  }

  it should "deserialize from Json correctly" in {
    (jsonYoutubeStats.as[YoutubeStatPane].right.value should ===(youtubeStats)) (youtubeStatsCanEqual)
  }

  it should "equal itself after reserialization" in {
    (youtubeStats.asJson.as[YoutubeStatPane].right.value should ===(youtubeStats)) (youtubeStatsCanEqual)
  }
}
