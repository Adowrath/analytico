package analytico
package macros

import scala.annotation.{ StaticAnnotation, compileTimeOnly }
import scala.collection.mutable
import scala.collection.immutable.Seq
import scala.meta._

object YTApiGenerator {

  /**
    * Ein kleiner DSL-Wrapper für die API-Generierung.
    *
    * Wird in der Runtime ''nicht'' vorkommen.
    *
    * @param s das Symbol.
    */
  @compileTimeOnly("Diese Klasse wird nur für die DSL des Makros benutzt. Makro-Paradise vielleicht nicht aktiviert?")
  final implicit class SymbolWrapper(val s: scala.Symbol) extends Dummy {
    /** Deine Parameter sind verschachtelt? */
    def apply(v: Any): this.type = impl(v)

    /** Implementierungs-Detail. */
    //noinspection NotImplementedCode
    override protected[YTApiGenerator] def impl(ignored: Any): Nothing = ???

    /** Für eine Property, die kein Part ist. */
    def unary_- : this.type = impl(())

    /** Für eine Property, die auch ein Part ist. */
    def unary_+ : this.type = impl(())
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
  class YouTubeApi extends StaticAnnotation {
    inline def apply(defn: Any): Any = meta {
      defn match {
        case q"..$mods object $ename extends { ..$stats } with ..$inits { $self => ..$stats1 }" =>
          expand(mods, ename, stats, inits, self, stats1)
        case _ =>
          abort(s"@ytApi must annotate an object definition with a non-empty body. ${defn.structure}")
      }
    }
  }

  def expand(mods: Seq[Mod],
             objectName: Term.Name,
             earlyInitializers: Seq[Stat],
             ancestors: Seq[Ctor.Call],
             selfType: Term.Param,
             stats: Seq[Stat]): Defn.Object = {
    val statements = for {
      stat ← stats
      property ← splitTree(stat).toList
      newStatement ← generateDefinitions(property)
    } yield newStatement

    val result =
      q"""..$mods object $objectName extends { ..$earlyInitializers } with ..$ancestors { $selfType =>
            ..$statements
          }"""

    println(result)
//    println(result.structure)

    result
  }

  /**
    * Entscheidet anhand des Methodennamens, ob die Eigenschaft auch ein Part der Anfrage ist.
    *
    * Die Methode `unary_-` stellt ein `-'symbol` dar, eine einfache Eigenschaft.
    *
    * Die Methode `unary_+` stellt ein `+'symbol` dar, eine Eigenschaft welche auch ein Part ist.
    */
  def isPart(name: Term.Name): Option[Boolean] = name match {
    case Term.Name("unary_-") ⇒ Some(false)
    case Term.Name("unary_+") ⇒ Some(true)
    case _ ⇒ None
  }

  /**
    * Analysiert den gegebenen Baum und erstellt aus ihm Properties.
    *
    * @param stat der Code, über den gearbeitet wird.
    *
    * @return eine Property
    */
  def splitTree(stat: Tree): Option[Property] = {
    val (name, method, inner) = stat match {
      case q"scala.Symbol(${symbolName @ Lit.String(_)}).$methodName" ⇒
        (symbolName, methodName, Nil)
      case q"scala.Symbol(${symbolName @ Lit.String(_)}){ ..$inners }.$methodName" ⇒
        (symbolName, methodName, inners map splitTree)
      case q"scala.Symbol(${symbolName @ Lit.String(_)})( $inner ).$methodName" ⇒
        (symbolName, methodName, splitTree(inner) :: Nil)

        // IntelliJ

      case Term.ApplyUnary(Term.Name(methodName), Term.Apply(Term.Select(Term.Name("scala"), Term.Name("Symbol")), Seq(Lit.String(symbolName)))) ⇒
        (Lit.String(symbolName.drop(1)), Term.Name(s"unary_$methodName"), Nil)
      case
        Term.ApplyUnary(Term.Name(methodName),Term.Apply(Term.Apply(Term.Select(Term.Name("scala"), Term.Name("Symbol")), Seq(Lit.String(symbolName))), Seq(Term.Block(inners)))) ⇒
        (Lit.String(symbolName.drop(1)), Term.Name(s"unary_$methodName"), inners map splitTree)
      case _ ⇒
        abort(s"Error: $stat, ${stat.structure}")
    }

    isPart(method) match {
      case None ⇒
        abort(stat.pos, s"The method name cannot be mapped to a valid state: `$method`. " +
          "Consider using either `unary_-` or `unary_+`.")
      case Some(isPart) ⇒
        Some(Property(stat.pos, isPart = Lit.Boolean(isPart), name, inner: _*))
    }
  }

  /** Das Klassensymbol eines einfachen Parameters. */
  lazy val simpleParam = Ctor.Ref.Name("_root_.analytico.youtube.apis.ApiParameter")
  /** Das Klassensymbol eines Parameters mit Kindern. */
  lazy val applicParam = Ctor.Ref.Name("_root_.analytico.youtube.apis.ApplicableParameter")

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
  def generateDefinitions(property: Property): Seq[Stat] = {
    /* Kleine Utility-Methode um den syntaktischen Noise in der `(emptyRightTreeBuffer /: seq)`-Zeile zu reduzieren. */
    def emptyRightTreeBuffer: Either[Unit, mutable.ListBuffer[Stat]] = Right(mutable.ListBuffer())

    val name = property.name
    val term = Pat.Var.Term(Term.Name(name.value))
    val objectName = Term.Name(name.value + "Parameters")
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
            abort(property.position,
              s"The property $name has an invalid child. Please check earlier error messages.")

          case Right(innerDefinitions) ⇒
            Seq(
              q"val $term = new $applicParam($name, $objectName, isPart = $isPart)",
              q"""
                 object $objectName {
                   ..${innerDefinitions.toList}
                 }"""
            )
        }
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
    *                 Wenn sie nicht leer ist, wird ein
    *                 [[analytico.youtube.apis.ApplicableParameter ApplicableParameter]] generiert,
    *                 samt eigenem Objekt mit allen verschachelten Eigenschaften.
    */
  case class Property(position: Position, isPart: Lit.Boolean, name: Lit.String, inners: Option[Property]*)

}
