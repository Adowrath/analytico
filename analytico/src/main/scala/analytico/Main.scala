package analytico

import java.io.{ IOException, PrintStream }

import analytico.youtube.YTAuth
import analytico.youtube.YTScope._
import com.google.api.services.youtubeAnalytics.YouTubeAnalytics
import com.google.api.services.youtubeAnalytics.model.ResultTable

import scala.collection.JavaConverters._

object Main {

  val start = "2017-11-13"
  val end = "2017-11-19"

  def main(args: Array[String]): Unit = {
    example()
  }

  /**
    * Kleiner Test der APIs.
    */
  def example(): Unit = {
    val api = YTAuth.authorize[YoutubeReadOnly && AnalyticsReadOnly]("analyticsAndYoutube")

    val analytics = api buildAnalytics "test"
    val youtube = api youtubeData "test"

    val channels = youtube.channels.mine.list(_.items(_.id, _.snippet(_.title, _.thumbnails(_.high))))

    println(channels)
    println(channels.getItems.get(0).getSnippet.getThumbnails.getHigh.getHeight)

    val listOfChannels = channels.getItems
    // The user's default channel is the first item in the list.
    val defaultChannel = listOfChannels.asScala.head

    val channelId = defaultChannel.getId

    //ViewCount fromResults executeViewsOverTimeQuery(analytics, channelId)
    printData(System.out, "Views Over Time.", executeViewsOverTimeQuery(analytics, channelId))
  }

  /**
    * Retrieve the views and unique viewers per day for the channel.
    *
    * @param analytics The service object used to access the Analytics API.
    * @param id        The channel ID from which to retrieve data.
    *
    * @return The API response.
    *
    * @throws IOException if an API error occurred.
    */
  @throws[IOException]
  private def executeViewsOverTimeQuery(analytics: YouTubeAnalytics, id: String): ResultTable = {
    analytics.reports
      .query("channel==" + id,
        start,
        end,
        "views,estimatedMinutesWatched,averageViewDuration")
      .setDimensions("liveOrOnDemand")
      .execute
  }

  /**
    * Prints the API response. The channel name is printed along with
    * each column name and all the data in the rows.
    *
    * @param writer  stream to output to
    * @param title   title of the report
    * @param results data returned from the API.
    */
  private def printData(writer: PrintStream, title: String, results: ResultTable): Unit = {
    writer.println("Report: " + title)

    // Falls es eine existente Liste ist die nicht leer ist, zu Scala umwandeln.
    Option(results.getRows).withFilter(!_.isEmpty).map(_.asScala) match {
      case None ⇒ writer.println("No results Found.")
      case Some(rows) ⇒
        val columnmHeaders = results.getColumnHeaders.asScala

        // Alle Header raus!
        for(header ← columnmHeaders) {
          writer.printf("%30s", header.getName)
        }
        writer.println()

        for(row ← rows) {
          for(i ← columnmHeaders.indices) {
            val header = columnmHeaders(i)
            val column = row.asScala(i)
            header.get("dataType") match {
              case "INTEGER" ⇒
                //noinspection ScalaMalformedFormatString
                writer.printf("%30d", Long.box(column.asInstanceOf[java.math.BigDecimal].longValue()))
              case "FLOAT" ⇒
                //noinspection ScalaMalformedFormatString
                writer.printf("%30.2f", column)
              case "STRING" ⇒
                writer.printf("%30s", column)
              case _ ⇒
                writer.printf("%30s", column)
            }
          }
          writer.println()
        }
        writer.println()
    }
  }

  final case class ViewCount(viewType: ViewCount.ViewType, views: BigDecimal, estimatedMinutes: BigDecimal, averageDuration: BigDecimal)

  object ViewCount {

    /**
      * Extrahiert die ViewCount-Resultate aus einer ResultTable.
      *
      * @param results die ResultTable von der YouTube-API.
      *
      * @return eine Liste aller ViewCounts.
      */
    def fromResults(results: ResultTable): Seq[ViewCount] = {
      results.getRows match {
        case null ⇒
          Nil
        case list ⇒
          list.asScala.map(_.asScala) map { row =>
            ViewCount(
              row.head match {
                case "LIVE" ⇒ Live
                case "ON_DEMAND" ⇒ OnDemand
              },
              BigDecimal.exact(row(1).asInstanceOf[java.math.BigDecimal]),
              BigDecimal.exact(row(2).asInstanceOf[java.math.BigDecimal]),
              BigDecimal.exact(row(3).asInstanceOf[java.math.BigDecimal]))
          }
      }
    }

    sealed trait ViewType

    object OnDemand extends ViewType

    object Live extends ViewType

  }

}
