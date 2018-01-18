package analytico
package ui

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random
import scalafx.beans.property.{ BooleanProperty, ObjectProperty, StringProperty }
import scalafx.collections.ObservableBuffer
import scalafx.scene.Node
import scalafx.scene.control._
import scalafx.scene.layout.Pane
import java.time.LocalDate

import com.google.api.services.youtubeAnalytics.YouTubeAnalytics

import analytico.Main.ViewCount._
import analytico.Main.{ ViewCount, datesOfYear }
import analytico.youtube.YTAuth
import analytico.youtube.YTScope._

/**
  * Eine Repräsentation eines Statistik-Panels. Siehe die konkreten Subklassen für Implementationen.
  */
trait StatPane {
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

  class YoutubeStatPane(val credentialsName: String,
                        displayName: String,
                        val channelId: String,
                        val analytics: YouTubeAnalytics)
    extends StatPane {

    private[this] val valid = BooleanProperty(false)

    val liveStats: BooleanProperty = BooleanProperty(true)
    val videoStats: BooleanProperty = BooleanProperty(false)
    val combined: BooleanProperty = BooleanProperty(false)
    val name = StringProperty(displayName)

    liveStats onInvalidate invalidate
    videoStats onInvalidate invalidate
    combined onInvalidate invalidate

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
      tab.text <==> name

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
              case Combined ⇒ combined()
            }
          }
        })
        tabView.editable = false
        tabView.columns.addAll(
          column("KW") { vc ⇒
            s"${vc.yearWeek.getWeek} (${vc.viewType})"
          },
          column("Aufrufe") { _.views.toBigInt },
          column("Durschnittliche Wiedergabedauer") { vc ⇒ secondsToString(vc.averageDuration) },
          column("Total Zuschauerzeit") { vc ⇒ secondsToString(vc.estimatedMinutes * 60) }
        )
        tabView
      }

      pane.foreach(_.children.addAll(settings(tab).map(_.delegate).asJava))
      valid() = true
    }

    def settings(tab: Tab): Seq[Node] = Seq({
      val cb = new CheckBox("Live-Stats einbeziehen?")
      cb.selected <==> liveStats
      cb
    }, {
      val cb = new CheckBox("On Demand einbeziehen?")
      cb.selected <==> videoStats
      cb
    }, button("Aktualisieren", valid) {
      initialize(tab, None)
    })
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
  }

  object NoStatsPane extends StatPane {
    override def initialize(tab: Tab, pane: Option[Pane]): Unit = ()
  }
}
