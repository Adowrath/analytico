package analytico

import scala.reflect.runtime.universe._
import scala.util.Try
import scalafx.Includes._
import scalafx.application.{ JFXApp, Platform }
import scalafx.scene.Scene
import scalafx.scene.image.Image
import scalafx.stage.Stage
import scalafxml.core.{ DependenciesByType, FXMLView }
import java.io.FileOutputStream
import java.time.{ DayOfWeek, LocalDate }

import org.apache.poi.ss.usermodel.{ CellStyle, HorizontalAlignment, Workbook }
import org.apache.poi.xssf.usermodel.{ XSSFCellStyle, XSSFWorkbook }
import org.threeten.extra.YearWeek

import analytico.data.ViewCount
import analytico.ui.StatPane
import analytico.ui.StatPane._
import better.files._
import io.circe.jawn._
import io.circe.syntax._

object Main extends JFXApp {

  stage = new JFXApp.PrimaryStage() {
    title = "Test window"
    icons += new Image(getClass.getResourceAsStream("/icon.png"))
    scene = new Scene(FXMLView(getClass.getResource("/main.fxml"), new DependenciesByType(Map(
      typeOf[Map[String, StatPane]] → loadData()
    ))))
    sizeToScene()
    Platform.runLater {
      minHeight = height()
      minWidth = width()
    }
  }

  def loadData(): Map[String, StatPane] = {
    if(defaultFile.exists) {
      val decodeResult = decodeFile[Map[String, StatPane]](defaultFile.toJava)

      decodeResult match {
        case Right(res) ⇒
          res
        case Left(err) ⇒
          println(err)
          Map()
      }
    } else {
      Map()
    }
  }

  def saveData(panes: collection.Map[String, StatPane], stage: Stage): Try[Unit] = Try {
    for(bw ← defaultFile.bufferedWriter) {
      bw.write(panes.asJson.spaces2)
    }
  }

  def username = System.getProperty("user.name")
  def dataFolder: File = file".data/$username"
  def defaultFile: File = dataFolder / "analytico.json"

  /** Ein kleiner Wrapper für eine lesbarere Syntax. Sollte 0 Overhead ausmachen! */
  implicit class WorkbookDecorator(val wb: Workbook) extends AnyVal {
    @inline
    def styleWithFormat(format: String): CellStyle = {
      val style = wb.createCellStyle
      style.setDataFormat(wb.getCreationHelper.createDataFormat getFormat format)
      style
    }
  }

  def generateSheets(liveName: String, onDemandName: String, completedName: String, counts: Seq[ViewCount]): Unit = {

    val columnHeaders = List(
      "KW" → 5,
      "Aufrufe" → 10,
      "Durschnittliche Wiedergabedauer" → 10,
      "Total Zuschauerzeit" → 10
    ).zipWithIndex

    val wb = new XSSFWorkbook()

    val liveSheet = wb.createSheet(liveName)
    val onDemandSheet = wb.createSheet(onDemandName)
    val completeSheet = wb.createSheet(completedName)

    val headerStyle = wb.createCellStyle
    headerStyle setRotation 90
    headerStyle setAlignment HorizontalAlignment.CENTER
    // KW oben links ist fett!
    val boldHeaderStyle = headerStyle.clone().asInstanceOf[XSSFCellStyle]
    boldHeaderStyle setFont {
      val boldFont = wb.createFont()
      boldFont.setBold(true)
      boldFont
    }

    for(sheet ← List(liveSheet, onDemandSheet, completeSheet)) {
      val row = sheet createRow 0
      for(((cellName, width), idx) ← columnHeaders) {
        val cell = row createCell idx
        cell setCellStyle headerStyle // Die Titel sind rotiert!
        cell setCellValue cellName
        sheet.setColumnWidth(idx, width * 256)
      }
      sheet.createFreezePane(0, 1)

      // KW oben links = Fettgedruckt!
      row getCell 0 setCellStyle boldHeaderStyle
    }


    val timeStyle = wb styleWithFormat "[h]:mm:ss"
    val viewsStyle = wb styleWithFormat "#,##0"

    for {
      (viewType, viewCounts) ← counts.groupBy(_.viewType)
      (viewCount, idx) ← viewCounts.sortBy(_.yearWeek).zipWithIndex
    } {
      val sheet = viewType match {
        case ViewCount.Live ⇒ liveSheet
        case ViewCount.Video ⇒ onDemandSheet
        case ViewCount.Combined ⇒ completeSheet
      }
      val row = sheet createRow (idx + 1)


      val cell0 = row createCell 0
      cell0 setCellValue viewCount.yearWeek.getWeek.toDouble

      val cell1 = row createCell 1
      cell1 setCellValue viewCount.views.toDouble
      cell1 setCellStyle viewsStyle

      val cell2 = row createCell 2
      cell2 setCellValue toExcelTime(viewCount.averageDuration)
      cell2 setCellStyle timeStyle

      val cell3 = row createCell 3
      cell3 setCellFormula s"B${idx + 2}*C${idx + 2}"
      cell3 setCellStyle timeStyle
    }

    val fileOut = new FileOutputStream("workbook.xlsx")
    wb.write(fileOut)
    fileOut.close()

    java.awt.Desktop.getDesktop.open(file"workbook.xlsx".toJava)
  }

  /** Wandelt eine Zeitangabe in Sekunden in die Darstellung von Excel um. */
  @inline
  private[this]
  def toExcelTime(inSeconds: BigDecimal): Double = (inSeconds / 60 / 60 / 24).doubleValue

  /**
    * Gibt ein Tuple aus Anfangs- und Enddatum eines ganzen Jahres zurück,
    * vom ersten Montag bis zum letzten Sonntag im Jahr.
    *
    * @param year das gefragte Jahr
    *
    * @return ein Tuple aus Anfangs- und Enddatum.
    */
  def datesOfYear(year: Int): (LocalDate, LocalDate) = {
    val firstWeek = YearWeek.of(year, 1)
    // Falls das Jahr 53 Wochen hat, nehmen wir die 53te Woche.
    val lastWeek = YearWeek.of(year, if(firstWeek.is53WeekYear()) 53 else 52)

    // Wochen gehen von Montag bis Sonntag, ihr Amis!
    (firstWeek atDay DayOfWeek.MONDAY, lastWeek atDay DayOfWeek.SUNDAY)
  }
}
