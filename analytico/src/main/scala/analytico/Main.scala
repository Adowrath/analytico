package analytico

import java.io.{ IOException, PrintStream }

import analytico.macros.YTApiGenerator._
import analytico.youtube.YTAuth
import analytico.youtube.YTScope._
import com.google.api.services.youtubeAnalytics.YouTubeAnalytics
import com.google.api.services.youtubeAnalytics.model.ResultTable

import scala.collection.JavaConverters._

object Main {

//  val dataAPI: Any = api {
//    -'items {
//      -'kind
//      +'id
//      +'snippet {
//        -'thumbnails
//      }
//    }
//  }



  @ytApi object Items {
    -'items
  }
  @ytApi object Items2 {
    +'items
  }
  @ytApi object Items3 {
    +'items
    +'etag
  }
  @ytApi object Items4 {
    +'items {
      +'snippet
    }
  }
  @ytApi object Items5 {
    +'items {
      -'snippet
      +'title
    }
  }


  def main(args: Array[String]): Unit = {
    println(Items.items)
    println(Items5.items)

//    println(m("aalpha"))
//    val a = ""
//    println(m(a + "f"))
//    m1(_.alpha)
//    m1(_.alpha(_.beta))
  }

  def example(): Unit = {
    val api = YTAuth.authorize[YoutubeReadOnly && AnalyticsReadOnly]("analyticsAndYoutube")

    val analytics = api buildAnalytics "test"

    val youtube = api buildDataAPI "test"

    val parts = List(
      //      "id",
      "snippet",
      "topicDetails"
    )

    // https://developers.google.com/youtube/v3/docs/channels
    val channelRequest = youtube.channels.list(parts mkString ",")
    channelRequest.setMine(true)
    channelRequest.setFields("items(id, snippet(title, thumbnails/high))")
    val channels = channelRequest.execute

    println(channels)
    println(channels.getItems.get(0).getSnippet.getThumbnails.getHigh.getHeight)

    val youtube2 = api yt "test"

    val channels2 = youtube2.channels.mine.list(_.items(_.id, _.snippet(_.thumbnails(_.high))))
    //val channels3 = youtube2.channels.mine.list(_.items2(_.id, _.snippet(_.thumbnails(_.high))))

    println(channels2)
    println(channels2.getItems.get(0).getSnippet.getThumbnails.getHigh.getHeight)

    val listOfChannels = channels.getItems
    // The user's default channel is the first item in the list.
    val defaultChannel = listOfChannels.asScala.head

    val channelId = defaultChannel.getId

    //ViewCount fromResults executeViewsOverTimeQuery(analytics, channelId)
    printData(System.out, "Views Over Time.", executeViewsOverTimeQuery(analytics, channelId))
  }


  val start = "2017-11-13"
  val end = "2017-11-19"

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

  final case class ViewCount(viewType: ViewCount.ViewType, views: BigDecimal, estimatedMinutes: BigDecimal, averageDuration: BigDecimal)

  object ViewCount {

    sealed trait ViewType

    object OnDemand extends ViewType

    object Live extends ViewType

    def fromResults(results: ResultTable): Seq[ViewCount] = {
      val r1 = results.getRows
      val r2 = Option(r1)
      val r3 = r2 map { _.asScala.toList }
      val r4 = r3 map { _ map { _.asScala.toList } }
      val r5 = r4 getOrElse Nil
      r5 map { row =>
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
}
