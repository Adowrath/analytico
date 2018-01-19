package analytico
package data

import scala.collection.JavaConverters._
import java.time.LocalDate

import com.google.api.services.youtubeAnalytics.model.ResultTable
import org.threeten.extra.YearWeek

import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._

/**
  * Eine Repräsentation der View-Statistiken in einer Woche, zusätzlich zur Information,
  * welcher [[analytico.data.ViewCount.ViewType Art]] die Zahlen sind.
  *
  * @param yearWeek         die Woche in einem Jahr.
  * @param viewType         die Art der Stats: Live, normale Videos, oder kombiniert?
  * @param views            die View-Zahlen.
  * @param estimatedMinutes die von YT geschätzte Zahl der geschauten Minuten.
  */
final case class ViewCount(yearWeek: YearWeek, viewType: ViewCount.ViewType, views: BigDecimal, estimatedMinutes: BigDecimal) {
  override def toString: String =
    s"ViewCount(yearWeek: $yearWeek, viewType: $viewType, views: $views, estimatedMinutes: $estimatedMinutes, avg: ${averageDuration}s)"

  /** Die ungefähre Zuschauzeit in Sekunden. */
  def averageDuration: BigDecimal = estimatedMinutes * 60 / views
}

object ViewCount {

  /** Kleines Alias für die "views" und "estimatedMinutes"-Werte. */
  private[this]
  type Stats = (BigDecimal, BigDecimal)
  /** Die zu erfüllende Struktur der ResultTable in [[fromResults]]. */
  private[this]
  val tableStructure = List("day", "liveOrOnDemand", "views", "estimatedMinutesWatched")
  /** 0-Wert. Zu simpel um dafür extra Scalaz-Monoide reinzuholen. */
  private[this]
  val emptyStats: Stats = (0, 0)

  /**
    * Extrahiert die ViewCount-Resultate aus einer ResultTable.
    *
    * ==Warnung==
    * Die ResultTable muss die Struktur "day", "liveOrOnDemand", "views", "estimatedMinutesWatched" haben!
    *
    * @param results die ResultTable von der YouTube-API.
    *
    * @return eine Liste aller ViewCounts.
    */
  def fromResults(results: ResultTable): Seq[ViewCount] = {
    require(results.getColumnHeaders.asScala.map(_.getName).toList === tableStructure,
      s"Die ResultTable muss die richtige Struktur haben: $tableStructure.\n" +
        s"Es war jedoch: ${results.getColumnHeaders.asScala.map(_.getName)}")

    Option(results.getRows).fold(Nil: Seq[ViewCount]) { list ⇒
      val weeklyStats = list.asScala.map(_.asScala) groupBy getYearWeek

      weeklyStats.flatMap { case (weekInYear, stats) ⇒
        stats.partition(_ (1) === "LIVE") match {
          case (liveStats, onDemandStats) ⇒
            collectStats(liveStats) → collectStats(onDemandStats) match {
              case ((liveViews, liveMinutes), (onDemandViews, onDemandMinutes)) ⇒
                // Alle 3 Arten der Views kombinieren.
                Seq(
                  ViewCount(weekInYear, Live, liveViews, liveMinutes),
                  ViewCount(weekInYear, Video, onDemandViews, onDemandMinutes),
                  ViewCount(weekInYear, Combined, liveViews + onDemandViews, liveMinutes + onDemandMinutes)
                )
            }
        }
      }.toSeq
        .sortBy(_.yearWeek)
    }
  }

  /** Parst das Jahr und die Woche aus der Liste heraus. */
  @SuppressWarnings(Array("org.wartremover.warts.ToString"))
  private[this]
  def getYearWeek(l: Seq[AnyRef]): YearWeek = YearWeek.from(LocalDate.parse(l.head.toString))

  /** Summiert die View-Zahlen und Minutenwerte über eine Woche hinweg. `AnyRef` da `ResultTable`. */
  private[this]
  def collectStats(stats: Seq[Seq[AnyRef]]): Stats =
    (emptyStats /: stats) {
      case ((views, minutes), row) ⇒
        (views + BigDecimal.exact(row(2).asInstanceOf[java.math.BigDecimal])) →
          (minutes + BigDecimal.exact(row(3).asInstanceOf[java.math.BigDecimal]))
    }

  /** Was für eine Art Statistik ist es? */
  sealed abstract class ViewType(override val toString: String)

  /** Zuschauerstatistiken für die Livestreams. */
  case object Live extends ViewType("Live")

  /** Zuschauerstatistiken für die normalen Videos. */
  case object Video extends ViewType("Video")

  /** Die gesamten Zuschauerstatistiken, Live und On Demand kombiniert. */
  case object Combined extends ViewType("Kombiniert")

  implicit val yearWeekEncoder: Encoder[YearWeek] = (a: YearWeek) => Json.obj(
    "year" → a.getYear.asJson,
    "week" → a.getWeek.asJson
  )
  implicit val yearWeekDecoder: Decoder[YearWeek] = (c: HCursor) => {
    (c.downField("year"), c.downField("week")) match {
      case (yc: HCursor, wc: HCursor) ⇒
        (yc.as[Int], wc.value.as[Int]) match {
          case (Right(y), Right(w)) ⇒ Right(YearWeek.of(y, w))
          case (l @ Left(_), Right(_)) ⇒ l.asInstanceOf[Either[DecodingFailure, YearWeek]]
          case (Right(_), l @ Left(_)) ⇒ l.asInstanceOf[Either[DecodingFailure, YearWeek]]
          case (Left(err), Left(err2)) ⇒
            Left(DecodingFailure(s"Multiple errors: ${err.message}, ${err2.message}", c.history))

        }
      case (_: HCursor, _) ⇒ Left(DecodingFailure("Decoding a YearWeek failed. Week missing.", c.history))
      case (_, _: HCursor) ⇒ Left(DecodingFailure("Decoding a YearWeek failed. Year missing.", c.history))
      case (_, _) ⇒ Left(DecodingFailure("Decoding a YearWeek failed. Year and Week missing.", c.history))
    }
  }

  implicit val viewCountEncoder: Encoder[ViewCount] = implicitly[Encoder[ViewCount]]
  implicit val viewCountDecoder: Decoder[ViewCount] = implicitly[Decoder[ViewCount]]

}
