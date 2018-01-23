package analytico

import scala.collection.JavaConverters._

import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtubeAnalytics.YouTubeAnalytics
import com.google.api.services.youtubeAnalytics.model.ResultTable
import com.google.api.services.youtubeAnalytics.model.ResultTable.ColumnHeaders

import io.circe.generic.auto._

trait YTApiMocks {
  def resultTable(rows: Seq[AnyRef]*): ResultTable = new ResultTable()
    .setColumnHeaders(List(
      new ColumnHeaders().setName("day"),
      new ColumnHeaders().setName("liveOrOnDemand"),
      new ColumnHeaders().setName("views"),
      new ColumnHeaders().setName("estimatedMinutesWatched")
    ).asJava)
    .setRows(rows.map(_.asJava).asJava)

  class MockAnalytics extends YouTubeAnalytics(new NetHttpTransport, new JacksonFactory, null) {
    override def reports(): Reports = new Reports {
      override def query(ids: String, startDate: String, endDate: String, metrics: String): Query = new Query(ids, startDate, endDate, metrics) {
        override def execute(): ResultTable = resultTable()
      }
    }
  }

}
