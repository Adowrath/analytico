package analytico.data

import org.scalatest._
import org.threeten.extra.YearWeek

import cats.{ Eq, Show }
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._

class ViewCountTests extends FlatSpec with Matchers with EitherValues {

  val viewCount: ViewCount = new ViewCount(YearWeek.of(1900, 1), ViewCount.Video, views = 1, estimatedMinutes = 1)
  val jsonViewCount: Json = Json.obj(
    "yearWeek" → Json.obj(
      "year" → 1900.asJson,
      "week" → 1.asJson
    ),
    "viewType" → Json.obj(
      "Video" → Json.obj()
    ),
    "views" → 1.asJson,
    "estimatedMinutes" → 1.asJson
  )
  val showRepr: String = "ViewCount(yearWeek: 1900-W01, viewType: Video, views: 1, estimatedMinutes: 1, avg: 60s)"

  "A ViewCount" should "serialize to Json correctly" in {
    viewCount.asJson should ===(jsonViewCount)
  }

  it should "deserialize from Json correctly" in {
    jsonViewCount.as[ViewCount].right.value should ===(viewCount)
  }

  it should "equal itself after reserialization" in {
    viewCount.asJson.as[ViewCount].right.value should ===(viewCount)
  }

  it should "have a working Show implementation" in {
    Show[ViewCount].show(viewCount) should ===(showRepr)
  }

  it should "have a working Eq implementation" in {
    Eq[ViewCount].eqv(viewCount, viewCount) should ===(true)
  }
}
