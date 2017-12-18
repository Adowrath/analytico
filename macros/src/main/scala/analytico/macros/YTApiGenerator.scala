package analytico
package macros

import analytico.youtube.apis.{ ApiParameter, ApplicableParameter }

import scala.annotation.{ StaticAnnotation, compileTimeOnly }
import scala.language.experimental.macros
import scala.reflect.macros._

object YTApiGenerator {

  //  @ytApi object ChannelRepresentation {
  //    -'kind
  //    -'etag
  //    -'nextPageToken
  //    -'prevPageToken
  //    -'pageInfo {
  //      -'totalResults
  //      -'resultsPerPage
  //    }
  //    -'items {
  //      -'kind
  //      -'etag
  //      +'id
  //      +'snippet {
  //        -'title
  //        -'description
  //        -'customUrl
  //        -'publishedAt
  //        -'thumbnails {
  //          -'high {
  //            -'url
  //            -'width
  //            -'height
  //          }
  //          -'medium {
  //            -'url
  //            -'width
  //            -'height
  //          }
  //          -'default {
  //            -'url
  //            -'width
  //            -'height
  //          }
  //        }
  //        -'defaultLanguage
  //        -'localized {
  //          -'title
  //          -'description
  //        }
  //        -'country
  //      }
  //      +'contentDetails {
  //        -'relatedPlaylists {
  //          -'likes
  //          -'favorites
  //          -'uploads
  //          -'watchHistory
  //          -'watchLater
  //        }
  //      }
  //      +'statistics {
  //        -'viewCount
  //        -'commentCount
  //        -'subscriberCount
  //        -'hiddenSubscriberCount
  //        -'videoCount
  //      }
  //      +'topicDetails {
  //        -'topicIds
  //        -'topicCategories
  //      }
  //      +'status {
  //        -'privacyStatus
  //        -'isLinked
  //        -'longUploadsStatus
  //      }
  //      +'brandingSettings {
  //        -'channel {
  //          -'title
  //          -'description
  //          -'keywords
  //          -'defaultTab
  //          -'trackingAnalyticsAccountId
  //          -'moderateComments
  //          -'showRelatedChannels
  //          -'showBrowseView
  //          -'featuredChannelsTitle
  //          -'featuredChannelsUrls
  //          -'unsubscribedTrailer
  //          -'profileColor
  //          -'defaultLanguage
  //          -'country
  //        }
  //        -'watch {
  //          -'textColor
  //          -'backgroundColor
  //          -'featuredPlaylistId
  //        }
  //        -'image {
  //          -'bannerImageUrl
  //          -'bannerMobileImageUrl
  //          -'watchIconImageUrl
  //          -'trackingImageUrl
  //          -'bannerTabletLowImageUrl
  //          -'bannerTabletImageUrl
  //          -'bannerTabletHdImageUrl
  //          -'bannerTabletExtraHdImageUrl
  //          -'bannerMobileLowImageUrl
  //          -'bannerMobileMediumHdImageUrl
  //          -'bannerMobileHdImageUrl
  //          -'bannerMobileExtraHdImageUrl
  //          -'bannerTvImageUrl
  //          -'bannerTvLowImageUrl
  //          -'bannerTvMediumImageUrl
  //          -'bannerTvHighImageUrl
  //          -'bannerExternalUrl
  //        }
  //        -'hints {
  //          -'property
  //          -'value
  //        }
  //      }
  //      +'auditDetails {
  //        -'overallGoodStanding
  //        -'communityGuidelinesGoodStanding
  //        -'copyrightStrikesGoodStanding
  //        -'contentIdClaimsGoodStanding
  //      }
  //      +'contentOwnerDetails {
  //        -'contentOwner
  //        -'timeLinked
  //      }
  //      +'localizations
  //    }
  //  }

  @compileTimeOnly("Should be removed after macro expansion.")
  implicit class SymbolWrapper(val s: Symbol) {
    private[this] def impl(ignored: Any = null): Nothing = throw new NotImplementedError("You shouldn't be here. This class shoudld have been destroyed after the macro expansion.")

    def apply(v: Any): this.type = impl(v)

    def unary_- : this.type = impl()

    def unary_+ : this.type = impl()
  }

  @compileTimeOnly("Macro Paradise ist nicht aktiviert; Die Annotation wurde nicht erased.")
  class ytApi extends StaticAnnotation {
    def macroTransform(annottees: Any*): Any = macro ApiGenerator.api_impl
  }

  class ApiGenerator(val c: whitebox.Context) {
    gen =>

    import c.universe._

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
      * Property(null, true, "hallo", Seq(Property(null, false, "inner")))
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
    case class Property(position: Position, isPart: Boolean, name: String, inners: Property*)

    object EmptyProperty extends Property(NoPosition, false, "")

    /**
      * Bricht die Ausführung des Makros mit einer Fehlermeldung ab.
      *
      * @param message die Nachricht
      *
      * @return Nichts.
      */
    def bail(message: String, pos: Position = c.enclosingPosition): Nothing = c.abort(pos, message)

    /** Entscheidet anhand des Methodennamens, ob die Eigenschaft auch ein Part der Anfrage ist.
      *
      * Die Methode `unary_$``minus` stellt ein `-'symbol` dar, eine einfache Eigenschaft.
      *
      * Die Methode `unary_$``plus`  stellt ein `+'symbol` dar, eine Eigenschaft welche auch ein Part ist.
      */
    def isPart(name: TermName): Option[Boolean] = name match {
      case TermName("unary_$minus") ⇒ Some(false)
      case TermName("unary_$plus") ⇒ Some(true)
      case _ ⇒ None
    }

    /**
      * Analysiert den gegebenen Baum und kreirt aus ihm Properties.
      *
      * @param tree der AST-Baum, über den gearbeitet wird.
      *
      * @return
      */
    def splitTree(tree: Tree): Property = {

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
          EmptyProperty
        case Some(isPart) ⇒
          Property(tree.pos, isPart = isPart, name.value.value.toString, inner: _*)
      }
    }

    lazy val simpleParam: ClassSymbol = symbolOf[ApiParameter].asClass
    lazy val applicParam: ClassSymbol = symbolOf[ApplicableParameter[_]].asClass

    def generateDefinitions(property: Property): Seq[Tree] = {
      property match {
        case EmptyProperty ⇒
          Nil
        case _ ⇒
          val name = property.name
          val term = TermName(name)
          val isPart = property.isPart
          property.inners match {
            case Seq() ⇒
              Seq(
                q"""
                   val $term = new $simpleParam($name, isPart = $isPart)
                 """
              )
            case seq ⇒
              val objectName = TermName(name + "Parameters")
              val innerDefinitions = seq flatMap generateDefinitions
              Seq(
                q"""
                   val $term = new $applicParam($name, $objectName, isPart = $isPart)
                 """,
                q"""
                   object $objectName {
                     ..$innerDefinitions
                   }
                 """
              )
          }
      }
    }

    /**
      * Die wirkliche Implementation des Makros.
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
    def api_impl(annottees: Expr[Any]*): Expr[Any] = {
      annottees.map(_.tree) match {
        case List(q"object $objectName extends $parent { ..${body: Seq[Tree]} }") if body.nonEmpty =>
          val properties = body map splitTree

          val statements = properties flatMap generateDefinitions

          c.Expr[Any](
            q"""
              object $objectName extends $parent {
                ..$statements
              }
            """)
        case _ =>
          bail("You must annotate an object definition with a non-empty body.")
      }
    }
  }

}
