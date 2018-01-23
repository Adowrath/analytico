package analytico
package ui

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random
import scalafx.beans.property.{ BooleanProperty, ObjectProperty }
import scalafx.collections.ObservableBuffer
import scalafx.scene.Node
import scalafx.scene.control._
import scalafx.scene.layout.Pane
import java.time.LocalDate

import com.google.api.services.youtubeAnalytics.YouTubeAnalytics
import org.scalactic.TypeCheckedTripleEquals._

import analytico.Main.datesOfYear
import analytico.data.ViewCount
import analytico.data.ViewCount._
import analytico.youtube.YTAuth
import analytico.youtube.YTScope._
import cats.Hash
import io.circe._
import io.circe.generic.auto._
import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }

/**
  * Eine Repräsentation eines Statistik-Panels. Siehe die konkreten Subklassen für Implementationen.
  */
sealed trait StatPane {
  /**
    * Füllt den gegebenen Tab mit der momentanen Statistik, das gegebene Pane mit den eigenen Buttons.
    *
    * @param tab der Tab, den man füllen soll.
    */
  def initialize(tab: Tab, pane: Option[Pane]): Unit
}

object StatPane {

  /**
    * Ein kleiner Helfer-Trait, um asynchrone Anfragen von Panes auf eine gewisse Weise canceln zu können.
    */
  trait Cancelable {
    def cancel(): Unit
  }

  final case class YoutubeStatPane(credentialsName: String,
                                   var displayName: String,
                                   channelId: String,
                                   var analytics: Option[YouTubeAnalytics],
                                   unsavedMarker: BooleanProperty)
    extends StatPane {

    private[this] val valid = BooleanProperty(false)

    def isValid: Boolean = valid()

    val liveStats: BooleanProperty = BooleanProperty(true)
    val videoStats: BooleanProperty = BooleanProperty(false)
    val combinedStats: BooleanProperty = BooleanProperty(false)

    liveStats onInvalidate invalidate
    videoStats onInvalidate invalidate
    combinedStats onInvalidate invalidate

    def invalidate(): Unit = {
      valid() = false
      unsavedMarker() = true
    }

    lazy val actualAnalytics: Future[YouTubeAnalytics] = analytics match {
      case Some(a) ⇒ Future.successful(a)
      case None ⇒
        YTAuth.authorize[AnalyticsReadOnly](credentialsName)._2.map(_.buildAnalytics("analytico-relogin"))
    }

    // TODO: Configurable dates.
    lazy val dates: (LocalDate, LocalDate) = datesOfYear(2018)

    lazy val data: Future[Seq[ViewCount]] = actualAnalytics map { analytics ⇒
      ViewCount.fromResults {
        analytics.reports
          .query("channel==" + channelId,
            dates._1.toString,
            dates._2.toString,
            "views,estimatedMinutesWatched")
          .setDimensions("day,liveOrOnDemand")
          .execute
      }
    }

    override def initialize(tab: Tab, pane: Option[Pane]): Unit = {
      tab.text() = displayName
      tab.text.onChange { (_, _, newName) ⇒
        displayName = newName
        unsavedMarker() = true
      }

      type T = ViewCount

      // TODO: Dies ist momentan nur für immutable Klassen!
      def column[R](name: String)(valueFactory: T ⇒ R): TableColumn[T, R] = {
        val col = new TableColumn[T, R](name)
        col.cellValueFactory = { c ⇒
          ObjectProperty(valueFactory(c.value))
        }
        col
      }

      data.foreach { counts ⇒
        tab.content = {
          val tabView = new TableView[ViewCount](ObservableBuffer {
            counts.filter {
              _.viewType match {
                case Live ⇒ liveStats()
                case Video ⇒ videoStats()
                case Combined ⇒ combinedStats()
              }
            }
          })
          tabView.editable = false

          /** Falls 2 oder mehr aktiv sind */
          if((liveStats() && (videoStats() || combinedStats())) || videoStats() && combinedStats()) {
            tabView.columns.add(
              column("Art") { _.viewType }
            )
          }
          tabView.columns.addAll(
            column("KW") { _.yearWeek.getWeek },
            column("Aufrufe") { _.views.toBigInt },
            column("Durschnittliche Wiedergabedauer") { vc ⇒ secondsToTime(vc.averageDuration.toBigInt()) },
            column("Total Zuschauerzeit") { vc ⇒ secondsToTime(vc.estimatedMinutes.toBigInt() * 60) }
          )
          tabView
        }
      }

      pane.foreach(_.children.addAll(settings(tab).map(_.delegate).asJava))
      valid() = true
    }

    def settings(tab: Tab): Seq[Node] = Seq(
      checkBox("Live-Stats einbeziehen?", checked = liveStats)(()),
      checkBox("On Demand einbeziehen?", checked = videoStats)(()),
      checkBox("Kombiniert einbeziehen?", checked = combinedStats)(()),
      button("Aktualisieren", disabled = valid) {
        initialize(tab, None)
      }
    )

    override def hashCode(): Int =
      YoutubeStatPane.youtubeStatPaneHash.hash(this)

    override def equals(that: Any): Boolean = that match {
      case that: YoutubeStatPane ⇒
        YoutubeStatPane.youtubeStatPaneHash.eqv(this, that)
      case _ ⇒ false
    }
  }

  object YoutubeStatPane {
    def apply(name: String, unsavedMarker: BooleanProperty): (Cancelable, Future[YoutubeStatPane]) = {
      val sanitizedName =
        f"${s"analytico${name.replaceAll("[^\\w]", "")}".take(22)}%s${Random.nextInt().toHexString}%8s".replace(' ', '0').take(30)

      val (receiver, auth) = YTAuth.authorize[AnalyticsReadOnly && YoutubeReadOnly](sanitizedName)
      val analytics = auth map { _ buildAnalytics "analytico" }
      val channelId = for {
        auth ← auth
        data = auth.youtubeData("analytico")
        channels ← data.channels.mine.list(_.items(_.id, _.snippet(_.title, _.thumbnails(_.high))))
      } yield channels
        .getItems
        .get(0)
        .getId

      (
        () => receiver.stop(),
        (channelId zipWith analytics) { (channelId, analytics) ⇒
          new YoutubeStatPane(sanitizedName, name, channelId, Some(analytics), unsavedMarker)
        }
      )
    }

    implicit val youtubeStatPaneEncoder: Encoder[YoutubeStatPane] =
      Encoder.forProduct6("credentials", "displayName", "channelId", "live-stats", "video-stats", "combined-stats") { ysp ⇒
        (ysp.credentialsName, ysp.displayName, ysp.channelId, ysp.liveStats(), ysp.videoStats(), ysp.combinedStats())
      }
    implicit val youtubeStatPaneDecoder: Decoder[YoutubeStatPane] =
      Decoder.forProduct6("credentials", "displayName", "channelId", "live-stats", "video-stats", "combined-stats") {
        (credentialsName: String, displayName: String, channelId: String, liveStats: Boolean, videoStats: Boolean, combinedStats: Boolean) ⇒
          val ysp = new YoutubeStatPane(credentialsName, displayName, channelId, None, BooleanProperty(false))
          ysp.liveStats() = liveStats
          ysp.videoStats() = videoStats
          ysp.combinedStats() = combinedStats
          ysp
      }

    /** Don't forget: Hash is also an Eq instance! */
    implicit val youtubeStatPaneHash: Hash[YoutubeStatPane] = new Hash[YoutubeStatPane] {
      override def hash(that: YoutubeStatPane): Int =
        that.credentialsName.hashCode ^
          that.displayName.hashCode ^
          that.channelId.hashCode

      override def eqv(left: YoutubeStatPane, right: YoutubeStatPane): Boolean =
        left.credentialsName === right.credentialsName &&
          left.displayName === right.displayName &&
          left.channelId === right.channelId
    }
  }

  object NoStatsPane extends StatPane {
    override def initialize(tab: Tab, pane: Option[Pane]): Unit = ()
  }

  implicit val statPaneEncoder: Encoder[StatPane] = deriveEncoder[StatPane]
  implicit val statPaneDecoder: Decoder[StatPane] = deriveDecoder[StatPane]

}
