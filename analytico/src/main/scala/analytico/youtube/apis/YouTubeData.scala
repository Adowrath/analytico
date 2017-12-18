package analytico.youtube.apis

import analytico.youtube.YTScope
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.ChannelListResponse

class YouTubeData[S <: YTScope](apiAccess: YouTube) {
  def channels: ChannelView = ChannelView()

  case class ChannelView(isMine: Boolean = false) {
    def mine: ChannelView = copy(isMine = true)

    def list(listItems: (ChannelListParams.type ⇒ ApiParameter)*): ChannelListResponse = {

      val parameters = listItems map { _ (ChannelListParams) }
      val fields = parameters map { _.repr } mkString ", "
      val parts = parameters flatMap { _.getParts } map { _.repr } mkString ","

      println(s"Fields: $fields")
      println(s"Parts: $parts")

      val channelRequest = apiAccess.channels.list(parts)
      channelRequest.setMine(isMine)
      channelRequest.setFields(fields)
      val channels = channelRequest.execute

      channels
    }
  }
}

object ChannelListParams {
  val items = new ApplicableParameter("items", itemsParameters, false)

  object itemsParameters {
    val kind = new ApiParameter("kind", false)
    val etag = new ApiParameter("etag", false)
    val id = new ApiParameter("id", true)
    val snippet = new ApplicableParameter("snippet", snippetParameters, true)

    object snippetParameters {
      val title = new ApiParameter("title", false)
      val description = new ApiParameter("description", false)
      val customUrl = new ApiParameter("customUrl", false)
      val publishedAt = new ApiParameter("publishedAt", false)
      val thumbnails = new ApplicableParameter("thumbnails", thumbnailsParameters, false)

      object thumbnailsParameters {
        val high = new ApplicableParameter("high", thumbnailDetailParameters, false)
        val medium = new ApplicableParameter("medium", thumbnailDetailParameters, false)
        val default = new ApplicableParameter("default", thumbnailDetailParameters, false)

        object thumbnailDetailParameters {
          val url = new ApiParameter("url", false)
          val width = new ApiParameter("width", false)
          val height = new ApiParameter("height", false)
        }
      }

      val defaultLanguage = new ApiParameter("defaultLanguage", false)
      val localized = new ApplicableParameter("localized", localizedParameters, false)

      object localizedParameters {
        val title = new ApiParameter("title", false)
        val description = new ApiParameter("description", false)
      }

      val country = new ApiParameter("country", false)
    }

    val contentDetails = new ApplicableParameter("contentDetails", contentDetailsParameters, true)

    object contentDetailsParameters {
      val relatedPlaylists = new ApplicableParameter("relatedPlaylists", relatedPlaylistsParameters, false)

      object relatedPlaylistsParameters {
        val likes = new ApiParameter("likes", false)
        val favorites = new ApiParameter("favorites", false)
        val uploads = new ApiParameter("uploads", false)
        val watchHistory = new ApiParameter("watchHistory", false)
        val watchLater = new ApiParameter("watchLater", false)
      }

    }

    val statistics = new ApplicableParameter("statistics", statisticsParameters, true)

    object statisticsParameters {
      val viewCount = new ApiParameter("viewCount", false)
      val commentCount = new ApiParameter("commentCount", false)
      val subscriberCount = new ApiParameter("subscriberCount", false)
      val hiddenSubscriberCount = new ApiParameter("hiddenSubscriberCount", false)
      val videoCount = new ApiParameter("videoCount", false)
    }

    val topicDetails = new ApplicableParameter("topicDetails", topicDetailsParameters, true)

    object topicDetailsParameters {
      val topicIds = new ApiParameter("topicIds", false)
      val topicCategories = new ApiParameter("topicCategories", false)
    }

    val status = new ApplicableParameter("status", statusParameters, true)

    object statusParameters {
      val privacyStatus = new ApiParameter("privacyStatus", false)
      val isLinked = new ApiParameter("isLinked", false)
      val longUploadsStatus = new ApiParameter("longUploadsStatus", false)
    }

    val brandingSettings = new ApplicableParameter("brandingSettings", brandingSettingsParameters, true)

    object brandingSettingsParameters {
      val channel = new ApplicableParameter("channel", channelParameters, false)

      object channelParameters {
        val title = new ApiParameter("title", false)
        val description = new ApiParameter("description", false)
        val keywords = new ApiParameter("keywords", false)
        val defaultTab = new ApiParameter("defaultTab", false)
        val trackingAnalyticsAccountId = new ApiParameter("trackingAnalyticsAccountId", false)
        val moderateComments = new ApiParameter("moderateComments", false)
        val showRelatedChannels = new ApiParameter("showRelatedChannels", false)
        val showBrowseView = new ApiParameter("showBrowseView", false)
        val featuredChannelsTitle = new ApiParameter("featuredChannelsTitle", false)
        val featuredChannelsUrls = new ApiParameter("featuredChannelsUrls", false)
        val unsubscribedTrailer = new ApiParameter("unsubscribedTrailer", false)
        val profileColor = new ApiParameter("profileColor", false)
        val defaultLanguage = new ApiParameter("defaultLanguage", false)
        val country = new ApiParameter("country", false)
      }

      val watch = new ApplicableParameter("watch", watchParameters, false)

      object watchParameters {
        val textColor = new ApiParameter("textColor", false)
        val backgroundColor = new ApiParameter("backgroundColor", false)
        val featuredPlaylistId = new ApiParameter("featuredPlaylistId", false)
      }

      val image = new ApplicableParameter("image", imageParameters, false)

      object imageParameters {
        val bannerImageUrl = new ApiParameter("bannerImageUrl", false)
        val bannerMobileImageUrl = new ApiParameter("bannerMobileImageUrl", false)
        val watchIconImageUrl = new ApiParameter("watchIconImageUrl", false)
        val trackingImageUrl = new ApiParameter("trackingImageUrl", false)
        val bannerTabletLowImageUrl = new ApiParameter("bannerTabletLowImageUrl", false)
        val bannerTabletImageUrl = new ApiParameter("bannerTabletImageUrl", false)
        val bannerTabletHdImageUrl = new ApiParameter("bannerTabletHdImageUrl", false)
        val bannerTabletExtraHdImageUrl = new ApiParameter("bannerTabletExtraHdImageUrl", false)
        val bannerMobileLowImageUrl = new ApiParameter("bannerMobileLowImageUrl", false)
        val bannerMobileMediumHdImageUrl = new ApiParameter("bannerMobileMediumHdImageUrl", false)
        val bannerMobileHdImageUrl = new ApiParameter("bannerMobileHdImageUrl", false)
        val bannerMobileExtraHdImageUrl = new ApiParameter("bannerMobileExtraHdImageUrl", false)
        val bannerTvImageUrl = new ApiParameter("bannerTvImageUrl", false)
        val bannerTvLowImageUrl = new ApiParameter("bannerTvLowImageUrl", false)
        val bannerTvMediumImageUrl = new ApiParameter("bannerTvMediumImageUrl", false)
        val bannerTvHighImageUrl = new ApiParameter("bannerTvHighImageUrl", false)
        val bannerExternalUrl = new ApiParameter("bannerExternalUrl", false)
      }

      val hints = new ApplicableParameter("hints", hintsParameters, false)

      object hintsParameters {
        val property = new ApiParameter("property", false)
        val value = new ApiParameter("value", false)
      }

    }

    val auditDetails = new ApplicableParameter("auditDetails", auditDetailsParameters, true)

    object auditDetailsParameters {
      val overallGoodStanding = new ApiParameter("overallGoodStanding", false)
      val communityGuidelinesGoodStanding = new ApiParameter("communityGuidelinesGoodStanding", false)
      val copyrightStrikesGoodStanding = new ApiParameter("copyrightStrikesGoodStanding", false)
      val contentIdClaimsGoodStanding = new ApiParameter("contentIdClaimsGoodStanding", false)
    }

    val contentOwnerDetails = new ApplicableParameter("contentOwnerDetails", contentOwnerDetailsParameters, true)

    object contentOwnerDetailsParameters {
      val contentOwner = new ApiParameter("contentOwner", false)
      val timeLinked = new ApiParameter("timeLinked", false)
    }

    val localizations = new ApiParameter("localizations", true)
  }
}
