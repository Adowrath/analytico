package analytico
package youtube
package apis

import scala.annotation.implicitNotFound
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.ChannelListResponse

import analytico.typelevel.Bool
import analytico.typelevel.Bool._

/**
  * Die typsicherere API zu YouTube Data.
  *
  * @param apiAccess der eigentlich unterliegende Access zu der API.
  * @tparam S die Scopes des Zugriffs.
  *
  * @groupname full-implemented Implementiert
  * @groupdesc full-implemented Die API wurde hier komplett implementiert.
  * @groupprio full-implemented 10
  *
  * @groupname part-implemented Teilweise implementiert
  * @groupdesc part-implemented Die API wurde hier bereits teilweise implementiert.
  * @groupprio part-implemented 20
  *
  * @groupname not-yet-implemented Nicht implementiert
  * @groupdesc not-yet-implemented Die API-Interfaces hier wurden noch nicht implementiert.
  * @groupprio not-yet-implemented 30
  */
@SuppressWarnings(Array("org.wartremover.warts.Nothing"))
class YouTubeData[S <: YTScope](apiAccess: YouTube) {

  /**
    * Ein Typ für eine unmögliche API-Realisierung.
    *
    * @group not-yet-implemented
    */
  @implicitNotFound("This API is not implemented yet.")
  type Impossible <: Nothing

  private[this] def fail[T](implicit evidence$1: Impossible): T = evidence$1

  /** @group part-implemented */
  def channels: ChannelView[False] = ChannelView[False]()

  /** @group part-implemented */
  case class ChannelView[FilterSpecified <: Bool](isMine: Option[Boolean] = None,
                                                  isForId: Option[String] = None,
                                                  isForUsername: Option[String] = None,
                                                  isForCategory: Option[String] = None) {
    type NoFilter = FilterSpecified =:= False
    type Filtered = FilterSpecified =:= True

    def mine(implicit notYetFiltered: NoFilter): ChannelView[True] =
      copy(isMine = Some(true))

    def categoryId(categoryId: String)(implicit notYetFiltered: NoFilter): ChannelView[True] =
      copy(isForCategory = Some(categoryId))

    def forUsername(forUsername: String)(implicit notYetFiltered: NoFilter): ChannelView[True] =
      copy(isForUsername = Some(forUsername))

    def id(id: String)(implicit notYetFiltered: NoFilter): ChannelView[True] =
      copy(isForId = Some(id))

    // TODO: managedByMe 	boolean

    def list(listItems: (ChannelRepresentation.type ⇒ ApiParameter)*)
            (implicit evidence: Filtered): Future[ChannelListResponse] = Future {

      val parameters = listItems.map(_ apply ChannelRepresentation)
      val fields = parameters.map(_.repr).mkString(", ")
      val parts = parameters.flatMap(_.parts).map(_.repr).mkString(",")

      val channelRequest = apiAccess.channels.list(parts)
      isMine foreach (channelRequest setMine _)
      isForId foreach channelRequest.setId
      isForUsername foreach channelRequest.setForUsername
      isForCategory foreach channelRequest.setCategoryId

      channelRequest.setFields(fields).execute()
    }


    def update(implicit impossible: Impossible): Impossible = impossible
  }

  /* --------------------------- *\
     --  NOT YET IMPLEMENTED  --
  \* --------------------------- */

  /** @group not-yet-implemented */
  def activities(implicit evidence$1: Impossible): ActivitiesView = fail
  /** @group not-yet-implemented */
  type ActivitiesView
  /** @group not-yet-implemented */
  def captions(implicit evidence$1: Impossible): CaptionsView = fail
  /** @group not-yet-implemented */
  type CaptionsView
  /** @group not-yet-implemented */
  def channelBanners(implicit evidence$1: Impossible): ChannelBannersView = fail
  /** @group not-yet-implemented */
  type ChannelBannersView
  /** @group not-yet-implemented */
  def channelSections(implicit evidence$1: Impossible): ChannelSectionsView = fail
  /** @group not-yet-implemented */
  type ChannelSectionsView
  /** @group not-yet-implemented */
  def comments(implicit evidence$1: Impossible): CommentsView = fail
  /** @group not-yet-implemented */
  type CommentsView
  /** @group not-yet-implemented */
  def commentThreads(implicit evidence$1: Impossible): CommentThreadsView = fail
  /** @group not-yet-implemented */
  type CommentThreadsView
  /** @group not-yet-implemented */
  def guideCategories(implicit evidence$1: Impossible): GuideCategoriesView = fail
  /** @group not-yet-implemented */
  type GuideCategoriesView
  /** @group not-yet-implemented */
  def i18nLanguages(implicit evidence$1: Impossible): i18nLanguagesView = fail
  /** @group not-yet-implemented */
  type i18nLanguagesView
  /** @group not-yet-implemented */
  def i18nRegions(implicit evidence$1: Impossible): i18nRegionsView = fail
  /** @group not-yet-implemented */
  type i18nRegionsView
  /** @group not-yet-implemented */
  def playlistItems(implicit evidence$1: Impossible): PlaylistItemsView = fail
  /** @group not-yet-implemented */
  type PlaylistItemsView
  /** @group not-yet-implemented */
  def playlists(implicit evidence$1: Impossible): PlaylistsView = fail
  /** @group not-yet-implemented */
  type PlaylistsView
  /** @group not-yet-implemented */
  def search(implicit evidence$1: Impossible): SearchView = fail
  /** @group not-yet-implemented */
  type SearchView
  /** @group not-yet-implemented */
  def subscriptions(implicit evidence$1: Impossible): SubscriptionsView = fail
  /** @group not-yet-implemented */
  type SubscriptionsView
  /** @group not-yet-implemented */
  def thumbnails(implicit evidence$1: Impossible): ThumbnailsView = fail
  /** @group not-yet-implemented */
  type ThumbnailsView
  /** @group not-yet-implemented */
  def videoAbuseReportReasons(implicit evidence$1: Impossible): VideoAbuseReportReasonsView = fail
  /** @group not-yet-implemented */
  type VideoAbuseReportReasonsView
  /** @group not-yet-implemented */
  def videoCategories(implicit evidence$1: Impossible): VideoCategoriesView = fail
  /** @group not-yet-implemented */
  type VideoCategoriesView
  /** @group not-yet-implemented */
  def videos(implicit evidence$1: Impossible): VideosView = fail
  /** @group not-yet-implemented */
  type VideosView
  /** @group not-yet-implemented */
  def watermarks(implicit evidence$1: Impossible): WatermarksView = fail
  /** @group not-yet-implemented */
  type WatermarksView

}
