package analytico
package youtube

import analytico.youtube.YTScope._
import analytico.youtube.apis._
import com.google.api.client.auth.oauth2.{ Credential ⇒ OAuthCredential }
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtubeAnalytics.YouTubeAnalytics

/**
  * Stellt einen Zugriffschnittstelle zu den YouTube-APIs dar.
  *
  * @tparam S der Scopes des Zugriffs.
  *
  * @since 28.11.2017
  * @version v1.0
  */
class YTAccess[S <: YTScope](val credential: OAuthCredential, val jsonFactory: JsonFactory, val httpTransport: HttpTransport)
  extends AnalyticsAccess
    with YoutubeDataAccess {
  override type Scopes = S
}

/**
  * Ein Trait, der die Youtube Analytics-API typ- und scopesicher angibt.
  * Vorübergehend nur ein Wrapper zum [[YouTubeAnalytics.Builder Builder]] der Google-Library.
  *
  * @since 29.11.2017
  * @version v1.0
  */
trait AnalyticsAccess extends GenericYTAccess {
  def buildAnalytics(applicationTitle: String)(implicit evidence: Requires[AnalyticsReadOnly]): YouTubeAnalytics =
    new YouTubeAnalytics.Builder(httpTransport, jsonFactory, credential)
      .setApplicationName(applicationTitle)
      .build()
}


/**
  * Ein Trait, der die Youtube Analytics-API typ- und scopesicher angibt.
  * Vorübergehend nur ein Wrapper zum [[YouTubeAnalytics.Builder Builder]] der Google-Library.
  *
  * @since 29.11.2017
  * @version v1.0
  */
trait YoutubeDataAccess extends GenericYTAccess {
  def youtubeData(applicationTitle: String)(implicit evidence: Requires[YoutubeReadOnly]): YouTubeData[Scopes] =
    new YouTubeData[Scopes](buildDataAPI(applicationTitle))

  def buildDataAPI(applicationTitle: String)(implicit evidence: Requires[YoutubeReadOnly]): YouTube =
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
    * Die vorhandenen Scopes in einer Instanz. Am besten wird ein Typ-Parameter hier zugewiesen.
    */
  type Scopes <: YTScope

  /**
    * Ein wenig Syntax Sugar für einen erforderlichen Scope.
    *
    * @tparam RequiredScope der erforderliche Scope.
    */
  type Requires[RequiredScope <: YTScope] = Scopes <:< RequiredScope

  /**
    * Das Credential
    *
    * @return
    */
  def credential: OAuthCredential

  def jsonFactory: JsonFactory

  def httpTransport: HttpTransport
}
