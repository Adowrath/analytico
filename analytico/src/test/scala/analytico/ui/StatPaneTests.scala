package analytico
package ui

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scalafx.beans.property.BooleanProperty

import com.google.api.services.youtubeAnalytics.YouTubeAnalytics
import org.scalactic.CanEqual
import org.scalatest._

import analytico.ui.StatPane.YoutubeStatPane
import analytico.youtube.YTAuth
import analytico.youtube.YTScope.AnalyticsReadOnly
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._

class StatPaneTests extends FlatSpec with Matchers with EitherValues with PrivateMethodTester {

  private val youtubeStatsCanEqual: YoutubeStatPane CanEqual YoutubeStatPane = { (left, right) ⇒
    left.credentialsName === right.credentialsName &&
      left.displayName === right.displayName &&
      left.channelId === right.channelId
  }

  val (_, awaitable) = YTAuth.authorize[AnalyticsReadOnly]("strangeName", reauthorize = false)
  val analytics: YouTubeAnalytics = Await.result(awaitable, Duration.Inf).buildAnalytics("strangeName")

  val youtubeStats: YoutubeStatPane = new YoutubeStatPane("credentialsName", "displayName", "channelId", analytics, BooleanProperty(true))
  val jsonYoutubeStats: Json = Json.obj(
    "credentials" → "credentialsName".asJson,
    "displayName" → "displayName".asJson,
    "channelId" → "channelId".asJson
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
