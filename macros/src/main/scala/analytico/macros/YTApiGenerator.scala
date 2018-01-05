package analytico
package macros

import analytico.youtube.apis.{ ApiParameter, ApplicableParameter }

import scala.annotation.{ StaticAnnotation, compileTimeOnly }
import scala.collection.mutable
import scala.language.experimental.macros
import scala.reflect.macros._

object YTApiGenerator {

  /**
    * Ein kleiner Wrapper, damit IntelliJ nicht meckert!
    *
    * @param c         der Compiler-Kontext. Achtung: Whitebox!
    * @param annottees die zu annotierenden Deklarationen.
    *
    * @return die transformierte und damit einsatzbereite API.
    *
    * @see [[http://docs.scala-lang.org/overviews/macros/annotations.html#walkthrough Scala Docs]] für eine Erklärung,
    *      warum es ein VarArgs-Parameter ist.
    * @see [[ytApi]] für das zugehörige Makro.
    */
  def api_impl(c: whitebox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = new YTApiGenerator[c.type](c).api_impl(annottees)

  /**
    * Ein kleiner DSL-Wrapper für die API-Generierung.
    *
    * Wird in der Runtime ''nicht'' vorkommen.
    *
    * @param s das Symbol.
    */
  @compileTimeOnly("Diese Klasse wird nur für die DSL des Makros benutzt. Makro-Paradise vielleicht nicht aktiviert?")
  final implicit class SymbolWrapper(val s: Symbol) extends Dummy {
    /** Deine Parameter sind verschachtelt? */
    def apply(v: Any): this.type = impl(v)

    /** Implementierungs-Detail. */
    //noinspection NotImplementedCode
    override protected[YTApiGenerator] def impl(ignored: Any = null): Nothing = ???

    /** Für eine Property, die kein Part ist. */
    def unary_- : this.type = impl()

    /** Für eine Property, die auch ein Part ist. */
    def unary_+ : this.type = impl()
  }

  /**
    * Scalac meckert sonst wegen nicht benutztem Argument.
    */
  @compileTimeOnly("Ein Dummy-Trait, damit bei impl nicht über das Nicht-Benutzen des Arguments gemeckert wird.")
  sealed trait Dummy {
    protected[YTApiGenerator] def impl(ignored: Any): Nothing
  }

  /**
    * Eine Annotation, um aus einem DSL-Objekt die entsprechende API zu generieren.
    *
    * @version v0.2
    */
  @compileTimeOnly("Macro Paradise ist nicht aktiviert; Die Annotation wurde nicht erased.")
  class ytApi extends StaticAnnotation {
    def macroTransform(annottees: Any*): Any = macro YTApiGenerator.api_impl
  }

}

/**
  * Ein Macro Bundle für die einfachere Bearbeitung.
  *
  * @param c der Kontext.
  * @tparam C der genaue Kontext
  *
  * @version v0.2
  */
class YTApiGenerator[C <: whitebox.Context](val c: C) {

  import c.universe._

  /** Das Klassensymbol eines einfachen Parameters. */
  lazy val simpleParam: ClassSymbol = symbolOf[ApiParameter].asClass
  /** Das Klassensymbol eines Parameters mit Kindern. */
  lazy val applicParam: ClassSymbol = symbolOf[ApplicableParameter[_]].asClass

  /**
    * Bricht die Ausführung des Makros mit einer Fehlermeldung ab.
    *
    * @param message die Nachricht
    *
    * @return Nichts.
    */
  def bail(message: String, pos: Position = c.enclosingPosition): Nothing = c.abort(pos, message)

  /**
    * Entscheidet anhand des Methodennamens, ob die Eigenschaft auch ein Part der Anfrage ist.
    *
    * Die Methode `unary_-` stellt ein `-'symbol` dar, eine einfache Eigenschaft.
    *
    * Die Methode `unary_+`  stellt ein `+'symbol` dar, eine Eigenschaft welche auch ein Part ist.
    */
  def isPart(name: TermName): Option[Boolean] = name.decodedName match {
    case TermName("unary_-") ⇒ Some(false)
    case TermName("unary_+") ⇒ Some(true)
    case _ ⇒ None
  }

  /**
    * Analysiert den gegebenen Baum und erstellt aus ihm Properties.
    *
    * @param tree der AST-Baum, über den gearbeitet wird.
    *
    * @return eine Property
    */
  def splitTree(tree: Tree): Option[Property] = {

    val (name, method, inner) = tree match {
      case q"scala.Symbol(${name: Literal}).${method: TermName}" ⇒
        (name, method, Nil)
      case q"scala.Symbol(${name: Literal})({..${inners: Seq[Tree]}}).${method: TermName}" ⇒
        (name, method, inners map splitTree)
    }

    isPart(method) match {
      case None ⇒
        c.error(tree.pos, s"The method name cannot be mapped to a valid state: `$method`. " +
          "Consider using either `unary_-` or `unary_+`.")
        None
      case Some(isPart) ⇒
        Some(Property(tree.pos, isPart = isPart, name.value.value.toString, inner: _*))
    }
  }

  /**
    * Generiert aus der gegebenen Property die benötigten ASTs.
    *
    * Aus einer Property ohne Kinder wird eine einfache `val`-Definition,
    * aus einer mit Kindern eine `val`-Definition und eine `object`-Definition.
    *
    * @param property die Property, aus der die Definitionen generiert werden sollen.
    *
    * @return eine Sequenz aus 1 oder 2 ASTs, je nach Art der Property.
    */
  def generateDefinitions(property: Property): Seq[Tree] = {
    /* Kleine Utility-Methode um den syntaktischen Noise in der `(emptyrightTreeBuffer /: seq)`-Zeile zu reduzieren. */
    def emptyRightTreeBuffer: Either[Unit, mutable.ListBuffer[Tree]] = Right(mutable.ListBuffer())

    val name = property.name
    val term = TermName(name)
    val isPart = property.isPart
    property.inners match {
      case Seq() ⇒
        Seq(q"val $term = new $simpleParam($name, isPart = $isPart)")
      case seq ⇒
        (emptyRightTreeBuffer /: seq) {
          case (_, None) | (_: Left[_, _], _) ⇒ Left(()) // Falls bereits ein Fehler besteht
          case (Right(acc), Some(innerProp)) ⇒ Right(acc ++= generateDefinitions(innerProp))
        } match {
          case _: Left[_, _] ⇒
            c.warning(property.position, s"The property $name has an invalid child. Please check earlier error messages.")
            Seq(q"val $term = ???")

          case Right(innerDefinitions) ⇒
            val objectName = TermName(name + "Parameters")
            Seq(
              q"val $term = new $applicParam($name, $objectName, isPart = $isPart)",
              q"""
               object $objectName {
                 ..$innerDefinitions
               }"""
            )
        }
    }
  }

  /**
    * Die grundlegende Implementation des Makros.
    *
    * @example
    * {{{
    *   // Aus diesem hier:
    *   @ytApi object ChannelRepresentation {
    *     -'kind
    *     -'etag
    *     -'nextPageToken
    *     -'prevPageToken
    *     -'pageInfo {
    *       -'totalResults
    *       -'resultsPerPage
    *     }
    *     -'items {
    *       -'kind
    *       -'etag
    *       +'id
    *       +'snippet {
    *         -'title
    *       }
    *     }
    *   }
    *
    *   // Soll das hier werden:
    *   object ChannelRepresentation {
    *     val kind = new ApiParameter("kind", isPart = false)
    *     val etag = new ApiParameter("etag", isPart = false)
    *     val nextPageToken = new ApiParameter("nextPageToken", isPart = false)
    *     val prevPageToken = new ApiParameter("prevPageToken", isPart = false)
    *     val pageInfo = new ApplicableParameter("pageInfo", pageInfoParameters, isPart = false)
    *     object pageInfoParameters {
    *       val totalResults = new ApiParameter("totalResults", isPart = false)
    *       val resultsPerPage = new ApiParameter("resultsPerPage", isPart = false)
    *     }
    *     val items = new ApplicableParameter("items", itemsParameters, isPart = false)
    *     object itemsParameters {
    *       val kind = new ApiParameter("kind", isPart = false)
    *       val etag = new ApiParameter("etag", isPart = false)
    *       val id = new ApiParameter("id", isPart = true)
    *       val snippet = new ApplicableParameter("snippet", snippetParameters, isPart = true)
    *       object snippetParameters {
    *         val title = new ApiParameter("title", isPart = false)
    *       }
    *     }
    *   }
    * }}}
    *
    * @param annottees die zu transformierenden Elemente. Danke dafür, Macro Paradise!
    *
    * @return das transformierte Element.
    */
  def api_impl(annottees: Seq[Expr[Any]]): Expr[Any] = {
    annottees.map(_.tree) match {
      case List(q"object $objectName extends $parent { ..${body: Seq[Tree]} }") if body.nonEmpty =>
        val properties = body flatMap splitTree

        val statements = properties flatMap generateDefinitions

        val result =
          q"""
             object $objectName extends $parent {
               ..$statements
             }"""

        println(showCode(result))

        c.Expr(result)
      case _ =>
        bail("You must annotate an object definition with a non-empty body.")
    }
  }

  /**
    * Eine Eigenschaft innerhalb der API.
    *
    * @example
    * {{{
    * Property(null, true, "hallo")
    * // wird zu
    *
    * val hallo = new ApiParameter("hallo", isPart = true)
    *
    * Property(null, true, "hallo", Property(null, false, "inner"))
    * // wird zu
    *
    * val hallo = new ApplicableParameter("hallo", halloParameters)
    * object halloParameters {
    *   val inner = new ApiParameter("inner", isPart = false)
    * }
    * }}}
    *
    * @param position die Position, für Fehlermeldungen.
    * @param isPart   ob die Eigenschaft auch ein Part ist.
    *                 Siehe [[analytico.youtube.apis.ApiParameter#isPart ApiParameter.isPart]].
    * @param name     der Name der Eigenschaft.
    * @param inners   allfällige innere Eigenschaften.
    *                 Wenn diese Liste leer ist, wird ein einfacher [[analytico.youtube.apis.ApiParameter ApiParameter]]
    *                 generiert.
    *                 Wenn sie nicht leer ist, wird ein [[analytico.youtube.apis.ApplicableParameter ApplicableParameter]]
    *                 generiert, samt eigenem Objekt mit allen verschachelten Eigenschaften.
    */
  case class Property(position: Position, isPart: Boolean, name: String, inners: Option[Property]*)

}
