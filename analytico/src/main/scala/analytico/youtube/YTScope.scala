package analytico
package youtube

import analytico.youtube.YTScope._

/**
  * Dieser ADT abstrahiert weg von den String-basierten Scopes, auf denen die YouTube-APIs basieren.
  *
  * Einzelne Scopes sind beim Aufrufen der [[YTAuth.authorize]]-Methode
  * als Typparameter anzugeben. Mehr dazu dort.
  *
  * @param scopes die Scopes, die schlussendlich von den APIs überprüft werden.
  *
  * @author Cyrill Brunner
  * @since 28.11.2017
  * @version 1.0
  */
sealed abstract class YTScope(val scopes: Set[String]) {
  /**
    * Der Resultierende Scope-Typ.
    * Ist dafür da, um `ScopeA && ScopeB` für das Credential in `ScopeA with ScopeB` umzuschreiben.
    */
  type Result <: YTScope

  override final def toString: String = s"YTScopes: ${scopes.mkString("\n          ")}"

  /** Convenience-Konstruktor für einen einzelnen Scope. */
  def this(singleScope: String) = this(Set(singleScope))

  /**
    * Kombinator für zwei Scopes, analog zum Typ [[YTScope#&& &&]].
    *
    * @param other der andere zu kombinierende Scope.
    *
    * @return ein Kombinierter Scope aus `this.type` und `other.type`.
    */
  final def &&(other: YTScope): this.type && other.type = new CombinedScope[this.type, other.type](this, other)
}

/**
  * Alle vorhandenen Scopes der YouTube-APIs.
  */
object YTScope {

  /**
    * Ein Alias für [[CombinedScope]]
    *
    * @tparam S1 der eine Scope Typ
    * @tparam S2 der andere Scope Typ
    */
  type &&[S1 <: YTScope, S2 <: YTScope] = CombinedScope[S1, S2]

  /**
    * Der implizite Konstruktor des kombinierten Scopes.
    *
    * @param scope1 der erste Scope der Kombination
    * @param scope2 der zweite Scope der Kombination
    * @tparam S1 der Typ des einen Scopes
    * @tparam S2 der Typ des anderen Scopes
    *
    * @return eine Instanz des Scope-Kombinierers [[YTScope.&&]]
    */
  implicit def multiScope[S1 <: YTScope, S2 <: YTScope](implicit scope1: S1, scope2: S2): S1 && S2 = scope1 && scope2

  /**
    * Der Scope für die YouTube Analytics API, Read-Only.
    *
    * @see [[https://developers.google.com/youtube/analytics/v1/data_model Google Analytics API]]
    */
  implicit object AnalyticsReadOnly extends YTScope("https://www.googleapis.com/auth/yt-analytics.readonly") {
    override type Result = AnalyticsReadOnly
  }

  type AnalyticsReadOnly = AnalyticsReadOnly.type

  /**
    * Der Scope für die YouTube Data API, Read-Only.
    *
    * @see [[https://developers.google.com/youtube/v3/getting-started Google Data API]]
    */
  implicit object YoutubeReadOnly extends YTScope("https://www.googleapis.com/auth/youtube.readonly") {
    override type Result = YoutubeReadOnly
  }

  type YoutubeReadOnly = YoutubeReadOnly.type

  /**
    * Eine Kombination aus einer Mehrzahl an Scopes.
    *
    * @param s1 der erste Scope
    * @param s2 der zweite Scope
    * @tparam S1 der Typ des ersten Scopes.
    * @tparam S2 der Typ des zweiten Scopes.
    */
  final class CombinedScope[+S1 <: YTScope, +S2 <: YTScope](val s1: S1, val s2: S2) extends YTScope(s1.scopes | s2.scopes) {

    /**
      * Kombiniert die Datentypen der zwei Scopes.
      *
      * @example
      * {{{
      * // implicitly[A =:= B] ist dafür da, um zu schauen, ob A und B derselbe Typ sind.
      *
      * implicitly[(S1 && S2)#Result  =:= S1 with S2]
      * // Ein einfaches && ist Äquivalent zu with.
      *
      * implicitly[((S1 && S2) && S3) =:= (S1 && (S2 && S3))] // Fehler: Nicht gleich.
      * // Während && nicht assoziativ ist...
      *
      * implicitly[((S1 && S2)  && S3) #Result =:= (S1 && (S2 && S3))#Result]
      * // ...führt ein Aufruf von #Result zu derselben flachen Struktur:
      *
      * implicitly[((S1 &&  S2) && S3) #Result =:= S1 with S2 with S3]
      * implicitly[( S1 && (S2  && S3))#Result =:= S1 with S2 with S3]
      * }}}
      */
    override type Result = s1.Result with s2.Result
  }

}
