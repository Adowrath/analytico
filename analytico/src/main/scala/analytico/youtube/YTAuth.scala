package analytico
package youtube

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import java.io.{ File, InputStreamReader }
import java.nio.file.Files

import com.google.api.client.auth.oauth2.StoredCredential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.{ GoogleAuthorizationCodeFlow, GoogleClientSecrets }
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.FileDataStoreFactory

/**
  * Enthält die Methode zur Authentifizierung mit der YT-API, `authorize`.
  *
  * @author Cyrill Brunner
  * @since 28.11.2017
  * @version 1.0
  */
object YTAuth {
  private[this] val credentialsDir = ".oauth-credentials"
  private[this] val httpTransport = new NetHttpTransport()
  private[this] val jsonFactory = new JacksonFactory()

  private[this] val userHome = System.getProperty("user.home")

  /**
    * Gibt ein für den angegebenen Scope authorisiertes Credential zurück.
    *
    * @example
    * {{{
    * import YTScope._
    * val credential = YTAuth.authorize[YoutubeReadOnly & AnalyticsReadOnly]("youtubeAndAnalytics")
    * // This credential now has access rights to YouTube and YouTube Analytics APIs in a read-only manner.
    * }}}
    *
    * @param datastoreName der Name der Datei, in der die API-Zugangsdaten persistiert werden.
    *                      Derselbe Datastore wird immer auf denselben User mit denselben API-Rechten weisen.
    *                      Folglich sollte der Datastore also eventuell gelöscht werden.
    * @param scopes        eine implizite Instanz des oder der angegebenen Scopes.
    * @tparam Scopes die Scopes für das angeforderte Credential
    *
    * @return ein Authentifiziertes Credential.
    *
    * @see [[https://github.com/youtube/api-samples/blob/42cc089cfb259825db0e23def48c369670d9af98/java/src/main/java/com/google/api/services/samples/youtube/cmdline/Auth.java#L48-L76 Source Sample von Google]]
    * @since 28.11.2017
    * @version 1.0
    */
  def authorize[Scopes <: YTScope](datastoreName: String, reauthorize: Boolean = false)
                                  (implicit scopes: Scopes): (LocalServerReceiver, Future[YTAccess.Apply[scopes.Result]]) = {
    val clientSecretReader = new InputStreamReader(getClass.getResourceAsStream("/client_secrets.json"))
    val clientSecrets = GoogleClientSecrets.load(jsonFactory, clientSecretReader)

    // Checks that the defaults have been replaced (Default = "Enter X here").
    if(clientSecrets.getDetails.getClientId.startsWith("Enter")
      || clientSecrets.getDetails.getClientSecret.startsWith("Enter ")) {
      System.out.println("Enter Client ID and Secret from " +
        "https://console.developers.google.com/project/_/apiui/credential into src/main/resources/client_secrets.json")
      System.exit(1)
    }


    val credentialsFile = new File(userHome + "/" + credentialsDir)
    // Falls eine Re-Authorisierung gewünscht ist.
    if(reauthorize)
      Files.deleteIfExists(credentialsFile.toPath.resolve(datastoreName))

    // This creates the credentials datastore at ~/.oauth-credentials/${datastoreName}
    val fileDataStoreFactory = new FileDataStoreFactory(credentialsFile)
    val datastore = fileDataStoreFactory.getDataStore[StoredCredential](datastoreName)

    val flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, jsonFactory, clientSecrets, scopes.scopes.asJava)
      .setCredentialDataStore(datastore)
      .build()

    // Build the local server and bind it to a random port
    val localReceiver = new LocalServerReceiver.Builder().setPort(-1).build
    // Authorize.
    (localReceiver,
      Future(YTAccess(
        credential = new AuthorizationCodeInstalledApp(flow, localReceiver).authorize("user"),
        jsonFactory = jsonFactory,
        httpTransport = httpTransport)(scopes)))
  }
}
