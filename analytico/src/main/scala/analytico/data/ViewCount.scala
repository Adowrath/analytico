package analytico
package data

import scala.collection.JavaConverters._
import scala.util.Try
import java.time.LocalDate

import com.google.api.services.youtubeAnalytics.model.ResultTable
import org.scalactic.Requirements._
import org.scalactic.TypeCheckedTripleEquals._
import org.threeten.extra.YearWeek

import cats.{ Hash, Show }
import io.circe._
import io.circe.generic.auto._
import io.circe.generic.semiauto._

/**
  * Eine Repräsentation der View-Statistiken in einer Woche, zusätzlich zur Information,
  * welcher [[analytico.data.ViewCount.ViewType Art]] die Zahlen sind.
  *
  * @param yearWeek         die Woche in einem Jahr.
  * @param viewType         die Art der Stats: Live, normale Videos, oder kombiniert?
  * @param views            die View-Zahlen.
  * @param estimatedMinutes die von YT geschätzte Zahl der geschauten Minuten.
  */
final case class ViewCount(yearWeek: YearWeek, viewType: ViewCount.ViewType,
                           views: BigDecimal, estimatedMinutes: BigDecimal) {
  override def toString: String =
    s"ViewCount(yearWeek: $yearWeek, viewType: $viewType, views: $views, " +
      s"estimatedMinutes: $estimatedMinutes, avg: ${Try(averageDuration.toString()).getOrElse("{NaN}")}s)"

  /** Die ungefähre Zuschauzeit in Sekunden. */
  lazy val averageDuration: BigDecimal =
    if(views === (0: BigDecimal))
      0
    else
      estimatedMinutes * 60 / views
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
      s"Die ResultTable muss die richtige Struktur haben!")

    Option(results.getRows).fold(Nil: Seq[ViewCount]) { list ⇒
      val weeklyStats = list.asScala.map(_.asScala) groupBy extractYearWeek

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
  def extractYearWeek(l: Seq[AnyRef]): YearWeek = YearWeek.from(LocalDate.parse(l.head.toString))

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

  implicit val yearWeekEncoder: Encoder[YearWeek] =
    Encoder.forProduct2("year", "week")(yw ⇒ (yw.getYear, yw.getWeek))

  implicit val yearWeekDecoder: Decoder[YearWeek] =
    Decoder.forProduct2("year", "week")(YearWeek.of)

  implicit val viewCountEncoder: Encoder[ViewCount] = deriveEncoder[ViewCount]
  implicit val viewCountDecoder: Decoder[ViewCount] = deriveDecoder[ViewCount]

  implicit val viewCountShow: Show[ViewCount] = Show.fromToString[ViewCount]
  /**
    * Wenn IntelliJ das ganze ausführt, findet es keine Methode:
    * `java.lang.NoSuchMethodError: cats.package$.Hash()Lcats/kernel/Hash$;`
    * Deswegen wird direkt der Kernel aufgerufen.
    */
  implicit val viewCountHash: Hash[ViewCount] = cats.kernel.Hash.fromUniversalHashCode[ViewCount]
}
