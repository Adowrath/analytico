package analytico
package data

import scala.collection.JavaConverters._
import java.time.DayOfWeek

import com.google.api.services.youtubeAnalytics.model.ResultTable
import com.google.api.services.youtubeAnalytics.model.ResultTable.ColumnHeaders
import org.scalatest._
import org.threeten.extra.YearWeek

import analytico.data.ViewCount.{ Combined, Live, Video }
import cats.{ Eq, Show }
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._

class ViewCountTests extends FlatSpec with Matchers with EitherValues with PrivateMethodTester {

  val firstWeek: YearWeek = YearWeek.of(1900, 1)
  val viewCount: ViewCount = new ViewCount(firstWeek, ViewCount.Video, views = 1, estimatedMinutes = 1)
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

  def resultTable(rows: Seq[AnyRef]*): ResultTable = new ResultTable()
    .setColumnHeaders(List(
      new ColumnHeaders().setName("day"),
      new ColumnHeaders().setName("liveOrOnDemand"),
      new ColumnHeaders().setName("views"),
      new ColumnHeaders().setName("estimatedMinutesWatched")
    ).asJava)
    .setRows(rows.map(_.asJava).asJava)

  def extractYearWeekMethod: PrivateMethod[YearWeek] = PrivateMethod[YearWeek]('extractYearWeek)

  def collectStatsMethod: PrivateMethod[(BigDecimal, BigDecimal)] = PrivateMethod[(BigDecimal, BigDecimal)]('collectStats)

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

  "The ViewCount object" should "require a good ResultTable column structure" in {
    assertThrows[IllegalArgumentException] {
      ViewCount fromResults resultTable().setColumnHeaders(List().asJava)
    }
  }

  it should "return an empty Seq with null rows" in {
    ViewCount fromResults resultTable().setRows(null) should ===(Seq())
  }

  it should "return an empty Seq with empty rows" in {
    ViewCount fromResults resultTable(Nil: _*) should ===(Seq())
  }

  it should "return a three-element Seq with a simple row" in {
    val table = resultTable(
      List("1900-01-01", "LIVE", (1: BigDecimal).bigDecimal, (1: BigDecimal).bigDecimal)
    )
    ViewCount fromResults table should ===(Seq(
      ViewCount(firstWeek, Live, 1, 1),
      ViewCount(firstWeek, Video, 0, 0),
      ViewCount(firstWeek, Combined, 1, 1)
    ))
  }

  it should "return a three-element Seq with multiple rows in one week and one category" in {
    val table = resultTable(
      List("1900-01-01", "LIVE", (1: BigDecimal).bigDecimal, (5: BigDecimal).bigDecimal),
      List("1900-01-01", "LIVE", (2: BigDecimal).bigDecimal, (5: BigDecimal).bigDecimal),
      List("1900-01-02", "LIVE", (3: BigDecimal).bigDecimal, (5: BigDecimal).bigDecimal),
      List("1900-01-03", "LIVE", (4: BigDecimal).bigDecimal, (5: BigDecimal).bigDecimal),
      List("1900-01-04", "LIVE", (5: BigDecimal).bigDecimal, (5: BigDecimal).bigDecimal)
    )
    ViewCount fromResults table should ===(Seq(
      ViewCount(firstWeek, Live, 15, 25),
      ViewCount(firstWeek, Video, 0, 0),
      ViewCount(firstWeek, Combined, 15, 25)
    ))
  }

  it should "combine stats accross two categories correctly" in {
    val table = resultTable(
      List("1900-01-01", "LIVE", (1: BigDecimal).bigDecimal, (5: BigDecimal).bigDecimal),
      List("1900-01-01", "ON_DEMAND", (99: BigDecimal).bigDecimal, (995: BigDecimal).bigDecimal)
    )
    ViewCount fromResults table should ===(Seq(
      ViewCount(firstWeek, Live, 1, 5),
      ViewCount(firstWeek, Video, 99, 995),
      ViewCount(firstWeek, Combined, 100, 1000)
    ))
  }

  it should "parse a year and week correctly" in {
    val parsed = ViewCount invokePrivate extractYearWeekMethod(Seq(firstWeek.atDay(DayOfWeek.MONDAY).toString))
    parsed should ===(firstWeek)
  }

  it should "return empty stats" in {
    val (total1, total2) = ViewCount invokePrivate collectStatsMethod(Seq())
    total1 should ===(0)
    total2 should ===(0)
  }

  it should "collect unary stats" in {
    val (total1, total2) = ViewCount invokePrivate collectStatsMethod(Seq(
      Seq((), (), BigDecimal(1).bigDecimal, BigDecimal(1).bigDecimal)
    ))
    total1 should ===(1)
    total2 should ===(1)
  }

  it should "collect simple stats" in {
    val (total1, total2) = ViewCount invokePrivate collectStatsMethod(Seq(
      Seq((), (), BigDecimal(1).bigDecimal, BigDecimal(1).bigDecimal),
      Seq((), (), BigDecimal(5).bigDecimal, BigDecimal(5).bigDecimal)
    ))
    total1 should ===(6)
    total2 should ===(6)
  }
}
