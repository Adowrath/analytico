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

import analytico.Main.datesOfYear
import analytico.data.ViewCount
import analytico.data.ViewCount._
import analytico.youtube.YTAuth
import analytico.youtube.YTScope._
import cats.Show
import io.circe.Decoder.Result
import io.circe._
import io.circe.generic.auto._
import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }
import io.circe.syntax._

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
                                   analytics: YouTubeAnalytics)
    extends StatPane {

    private[this] val valid = BooleanProperty(false)

    val liveStats: BooleanProperty = BooleanProperty(true)
    val videoStats: BooleanProperty = BooleanProperty(false)
    val combinedStats: BooleanProperty = BooleanProperty(false)

    liveStats onInvalidate invalidate
    videoStats onInvalidate invalidate
    combinedStats onInvalidate invalidate

    def invalidate(): Unit = {
      valid() = false
    }

    // TODO: Configurable dates.
    lazy val dates: (LocalDate, LocalDate) = datesOfYear(2018)

    lazy val counts: Seq[ViewCount] = ViewCount.fromResults {
      analytics.reports
        .query("channel==" + channelId,
          dates._1.toString,
          dates._2.toString,
          "views,estimatedMinutesWatched")
        .setDimensions("day,liveOrOnDemand")
        .execute
    }

    override def initialize(tab: Tab, pane: Option[Pane]): Unit = {
      tab.text() = displayName
      tab.text.onChange { (_, _, newName) ⇒
        displayName = newName
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

      def secondsToString(seconds: BigDecimal): String = {
        val s = seconds % 60 toBigInt()
        val minutes = (seconds.toBigInt() - s) / 60
        val m = minutes % 60
        val h = (minutes - m) / 60

        f"$h%d:$m%02d:$s%02d"
      }

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
          column("Durschnittliche Wiedergabedauer") { vc ⇒ secondsToString(vc.averageDuration) },
          column("Total Zuschauerzeit") { vc ⇒ secondsToString(vc.estimatedMinutes * 60) }
        )
        tabView
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
  }

  object YoutubeStatPane {
    def apply(name: String): (Cancelable, Future[YoutubeStatPane]) = {
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
        (channelId zipWith analytics) { new YoutubeStatPane(sanitizedName, name, _, _) }
      )
    }

    private[StatPane] implicit val youtubeStatPaneEncoder: Encoder[YoutubeStatPane] = (statPane: YoutubeStatPane) => Json.obj(
      "credentials" → statPane.credentialsName.asJson,
      "displayName" → statPane.displayName.asJson,
      "channelId" → statPane.channelId.asJson
    )
    private[StatPane] implicit val youtubeStatPaneDecoder: Decoder[YoutubeStatPane] = { (c: HCursor) ⇒
      val failureShow = Show[DecodingFailure]
      import failureShow.show
      @inline
      def get[R: Decoder](name: String): Result[R] = c.downField(name).as[R]

      def failures2(err1: DecodingFailure, err2: DecodingFailure) = Left(DecodingFailure(
        s"""Multiple errors (2):
           | - ${show(err1)}
           | - ${show(err2)}""".stripMargin, c.history))

      def failures3(err1: DecodingFailure, err2: DecodingFailure, err3: DecodingFailure) = Left(DecodingFailure(
        s"""Multiple errors (3):
           | - ${show(err1)}
           | - ${show(err2)}
           | - ${show(err3)}""".stripMargin, c.history))

      (get[String]("credentials"), get[String]("displayName"), get[String]("channelId")) match {
        case (Right(cred), Right(name), Right(channelId)) ⇒ Right(
          // TODO
          new YoutubeStatPane(cred, name, channelId, null)
        )

        case (l @ Left(_), _ @ Right(_), _ @ Right(_)) ⇒ l.asInstanceOf[Result[YoutubeStatPane]]
        case (_ @ Right(_), l @ Left(_), _ @ Right(_)) ⇒ l.asInstanceOf[Result[YoutubeStatPane]]
        case (_ @ Right(_), _ @ Right(_), l @ Left(_)) ⇒ l.asInstanceOf[Result[YoutubeStatPane]]

        case (Right(_), Left(err), Left(err2)) ⇒ failures2(err, err2)
        case (Left(err), Right(_), Left(err2)) ⇒ failures2(err, err2)
        case (Left(err), Left(err2), Right(_)) ⇒ failures2(err, err2)

        case (Left(err), Left(err2), Left(err3)) ⇒ failures3(err, err2, err3)
      }
    }
  }

  object NoStatsPane extends StatPane {
    override def initialize(tab: Tab, pane: Option[Pane]): Unit = ()
  }

  implicit val statPaneEncoder: Encoder[StatPane] = deriveEncoder[StatPane]
  implicit val statPaneDecoder: Decoder[StatPane] = deriveDecoder[StatPane]

}
