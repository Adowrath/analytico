package analytico
package youtube

import com.google.api.client.auth.oauth2.{ Credential ⇒ OAuthCredential }
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtubeAnalytics.YouTubeAnalytics

import analytico.youtube.YTScope._
import analytico.youtube.apis._

/**
  * Stellt eine Zugriffschnittstelle zu den YouTube-APIs dar.
  *
  * @since 28.11.2017
  * @version v1.0
  */
class YTAccess(val credential: OAuthCredential, val jsonFactory: JsonFactory, val httpTransport: HttpTransport)
  extends GenericYTAccess
    with AnalyticsAccess
    with YoutubeDataAccess

object YTAccess {
  type Apply[S <: YTScope] = YTAccess {type Scopes = S}

  def apply[Scopes <: YTScope](credential: OAuthCredential,
                               jsonFactory: JsonFactory,
                               httpTransport: HttpTransport)
                              (implicit scopes: Scopes): Apply[scopes.Result] = {
    new YTAccess(
      credential = credential,
      jsonFactory = jsonFactory,
      httpTransport = httpTransport)
      .asInstanceOf[Apply[scopes.Result]]
  }
}

/**
  * Ein Trait, der die Youtube Analytics-API typ- und scopesicher angibt.
  * Vorübergehend nur ein Wrapper zum [[com.google.api.services.youtubeAnalytics.YouTubeAnalytics.Builder Builder]] der Google-Library.
  *
  * @since 29.11.2017
  * @version v1.0
  */
trait AnalyticsAccess { self: GenericYTAccess ⇒
  def buildAnalytics(applicationTitle: String)(implicit evidence: Req[AnalyticsReadOnly]): YouTubeAnalytics =
    new YouTubeAnalytics.Builder(httpTransport, jsonFactory, credential)
      .setApplicationName(applicationTitle)
      .build()
}


/**
  * Ein Trait, der die Youtube Data-API typ- und scopesicher angibt.
  * Vorübergehend nur ein Wrapper zum [[com.google.api.services.youtube.YouTube.Builder Builder]] der Google-Library.
  *
  * @since 29.11.2017
  * @version v1.0
  */
trait YoutubeDataAccess { self: GenericYTAccess ⇒
  def youtubeData(applicationTitle: String)(implicit evidence: Req[YoutubeReadOnly]): YouTubeData[Scopes] =
    new YouTubeData[Scopes](buildDataAPI(applicationTitle))

  def buildDataAPI(applicationTitle: String)(implicit evidence: Req[YoutubeReadOnly]): YouTube =
    new YouTube.Builder(httpTransport, jsonFactory, credential)
      .setApplicationName(applicationTitle)
      .build()
}

/**
  * Eine generische Elternklasse für die YT-API-Zugriffe, welche die erforderlichen Methoden und Typen festlegt.
  *
  * So muss z.B. jeder Access `Credential`s haben und über eine `JsonFactory` und einen `HttpTransport`
  * verfügen, um Anfragen an die API durchführen zu können..
  */
trait GenericYTAccess {
  /**
    * Ein wenig Syntax Sugar für einen erforderlichen Scope.
    *
    * @tparam RequiredScope der erforderliche Scope.
    */
  type Req[RequiredScope <: YTScope] = Scopes <:< RequiredScope
  /**
    * Die vorhandenen Scopes in einer Instanz.
    */
  protected[this] type Scopes <: YTScope

  /**
    * Das Credential
    *
    * @return
    */
  def credential: OAuthCredential

  def jsonFactory: JsonFactory

  def httpTransport: HttpTransport
}
