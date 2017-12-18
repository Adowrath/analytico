package analytico
package youtube
package apis

/**
  * Das Channel-Objekt in der YouTube Data API.
  *
  * Eine eigentliche Lösung über ein Makro ist unten zu sehen.
  *
  * @see [[https://developers.google.com/youtube/v3/docs/channels Google Docs]]
  */
object ChannelRepresentation {
  val kind = new ApiParameter("kind", isPart = false)
  val etag = new ApiParameter("etag", isPart = false)
  val nextPageToken = new ApiParameter("nextPageToken", isPart = false)
  val prevPageToken = new ApiParameter("prevPageToken", isPart = false)
  val pageInfo = new ApplicableParameter("pageInfo", pageInfoParameters, isPart = false)

  object pageInfoParameters {
    val totalResults = new ApiParameter("totalResults", isPart = false)
    val resultsPerPage = new ApiParameter("resultsPerPage", isPart = false)
  }

  val items = new ApplicableParameter("items", itemsParameters, isPart = false)

  object itemsParameters {
    val kind = new ApiParameter("kind", isPart = false)
    val etag = new ApiParameter("etag", isPart = false)
    val id = new ApiParameter("id", isPart = true)
    val snippet = new ApplicableParameter("snippet", snippetParameters, isPart = true)

    object snippetParameters {
      val title = new ApiParameter("title", isPart = false)
      val description = new ApiParameter("description", isPart = false)
      val customUrl = new ApiParameter("customUrl", isPart = false)
      val publishedAt = new ApiParameter("publishedAt", isPart = false)
      val thumbnails = new ApplicableParameter("thumbnails", thumbnailsParameters, isPart = false)

      object thumbnailsParameters {
        val high = new ApplicableParameter("high", highParameters, isPart = false)

        object highParameters {
          val url = new ApiParameter("url", isPart = false)
          val width = new ApiParameter("width", isPart = false)
          val height = new ApiParameter("height", isPart = false)
        }

        val medium = new ApplicableParameter("medium", mediumParameters, isPart = false)

        object mediumParameters {
          val url = new ApiParameter("url", isPart = false)
          val width = new ApiParameter("width", isPart = false)
          val height = new ApiParameter("height", isPart = false)
        }

        val default = new ApplicableParameter("default", defaultParameters, isPart = false)

        object defaultParameters {
          val url = new ApiParameter("url", isPart = false)
          val width = new ApiParameter("width", isPart = false)
          val height = new ApiParameter("height", isPart = false)
        }

      }

      val defaultLanguage = new ApiParameter("defaultLanguage", isPart = false)
      val localized = new ApplicableParameter("localized", localizedParameters, isPart = false)

      object localizedParameters {
        val title = new ApiParameter("title", isPart = false)
        val description = new ApiParameter("description", isPart = false)
      }

      val country = new ApiParameter("country", isPart = false)
    }

    val contentDetails = new ApplicableParameter("contentDetails", contentDetailsParameters, isPart = true)

    object contentDetailsParameters {
      val relatedPlaylists = new ApplicableParameter("relatedPlaylists", relatedPlaylistsParameters, isPart = false)

      object relatedPlaylistsParameters {
        val likes = new ApiParameter("likes", isPart = false)
        val favorites = new ApiParameter("favorites", isPart = false)
        val uploads = new ApiParameter("uploads", isPart = false)
        val watchHistory = new ApiParameter("watchHistory", isPart = false)
        val watchLater = new ApiParameter("watchLater", isPart = false)
      }

    }

    val statistics = new ApplicableParameter("statistics", statisticsParameters, isPart = true)

    object statisticsParameters {
      val viewCount = new ApiParameter("viewCount", isPart = false)
      val commentCount = new ApiParameter("commentCount", isPart = false)
      val subscriberCount = new ApiParameter("subscriberCount", isPart = false)
      val hiddenSubscriberCount = new ApiParameter("hiddenSubscriberCount", isPart = false)
      val videoCount = new ApiParameter("videoCount", isPart = false)
    }

    val topicDetails = new ApplicableParameter("topicDetails", topicDetailsParameters, isPart = true)

    object topicDetailsParameters {
      val topicIds = new ApiParameter("topicIds", isPart = false)
      val topicCategories = new ApiParameter("topicCategories", isPart = false)
    }

    val status = new ApplicableParameter("status", statusParameters, isPart = true)

    object statusParameters {
      val privacyStatus = new ApiParameter("privacyStatus", isPart = false)
      val isLinked = new ApiParameter("isLinked", isPart = false)
      val longUploadsStatus = new ApiParameter("longUploadsStatus", isPart = false)
    }

    val brandingSettings = new ApplicableParameter("brandingSettings", brandingSettingsParameters, isPart = true)

    object brandingSettingsParameters {
      val channel = new ApplicableParameter("channel", channelParameters, isPart = false)

      object channelParameters {
        val title = new ApiParameter("title", isPart = false)
        val description = new ApiParameter("description", isPart = false)
        val keywords = new ApiParameter("keywords", isPart = false)
        val defaultTab = new ApiParameter("defaultTab", isPart = false)
        val trackingAnalyticsAccountId = new ApiParameter("trackingAnalyticsAccountId", isPart = false)
        val moderateComments = new ApiParameter("moderateComments", isPart = false)
        val showRelatedChannels = new ApiParameter("showRelatedChannels", isPart = false)
        val showBrowseView = new ApiParameter("showBrowseView", isPart = false)
        val featuredChannelsTitle = new ApiParameter("featuredChannelsTitle", isPart = false)
        val featuredChannelsUrls = new ApiParameter("featuredChannelsUrls", isPart = false)
        val unsubscribedTrailer = new ApiParameter("unsubscribedTrailer", isPart = false)
        val profileColor = new ApiParameter("profileColor", isPart = false)
        val defaultLanguage = new ApiParameter("defaultLanguage", isPart = false)
        val country = new ApiParameter("country", isPart = false)
      }

      val watch = new ApplicableParameter("watch", watchParameters, isPart = false)

      object watchParameters {
        val textColor = new ApiParameter("textColor", isPart = false)
        val backgroundColor = new ApiParameter("backgroundColor", isPart = false)
        val featuredPlaylistId = new ApiParameter("featuredPlaylistId", isPart = false)
      }

      val image = new ApplicableParameter("image", imageParameters, isPart = false)

      object imageParameters {
        val bannerImageUrl = new ApiParameter("bannerImageUrl", isPart = false)
        val bannerMobileImageUrl = new ApiParameter("bannerMobileImageUrl", isPart = false)
        val watchIconImageUrl = new ApiParameter("watchIconImageUrl", isPart = false)
        val trackingImageUrl = new ApiParameter("trackingImageUrl", isPart = false)
        val bannerTabletLowImageUrl = new ApiParameter("bannerTabletLowImageUrl", isPart = false)
        val bannerTabletImageUrl = new ApiParameter("bannerTabletImageUrl", isPart = false)
        val bannerTabletHdImageUrl = new ApiParameter("bannerTabletHdImageUrl", isPart = false)
        val bannerTabletExtraHdImageUrl = new ApiParameter("bannerTabletExtraHdImageUrl", isPart = false)
        val bannerMobileLowImageUrl = new ApiParameter("bannerMobileLowImageUrl", isPart = false)
        val bannerMobileMediumHdImageUrl = new ApiParameter("bannerMobileMediumHdImageUrl", isPart = false)
        val bannerMobileHdImageUrl = new ApiParameter("bannerMobileHdImageUrl", isPart = false)
        val bannerMobileExtraHdImageUrl = new ApiParameter("bannerMobileExtraHdImageUrl", isPart = false)
        val bannerTvImageUrl = new ApiParameter("bannerTvImageUrl", isPart = false)
        val bannerTvLowImageUrl = new ApiParameter("bannerTvLowImageUrl", isPart = false)
        val bannerTvMediumImageUrl = new ApiParameter("bannerTvMediumImageUrl", isPart = false)
        val bannerTvHighImageUrl = new ApiParameter("bannerTvHighImageUrl", isPart = false)
        val bannerExternalUrl = new ApiParameter("bannerExternalUrl", isPart = false)
      }

      val hints = new ApplicableParameter("hints", hintsParameters, isPart = false)

      object hintsParameters {
        val property = new ApiParameter("property", isPart = false)
        val value = new ApiParameter("value", isPart = false)
      }

    }

    val auditDetails = new ApplicableParameter("auditDetails", auditDetailsParameters, isPart = true)

    object auditDetailsParameters {
      val overallGoodStanding = new ApiParameter("overallGoodStanding", isPart = false)
      val communityGuidelinesGoodStanding = new ApiParameter("communityGuidelinesGoodStanding", isPart = false)
      val copyrightStrikesGoodStanding = new ApiParameter("copyrightStrikesGoodStanding", isPart = false)
      val contentIdClaimsGoodStanding = new ApiParameter("contentIdClaimsGoodStanding", isPart = false)
    }

    val contentOwnerDetails = new ApplicableParameter("contentOwnerDetails", contentOwnerDetailsParameters, isPart = true)

    object contentOwnerDetailsParameters {
      val contentOwner = new ApiParameter("contentOwner", isPart = false)
      val timeLinked = new ApiParameter("timeLinked", isPart = false)
    }

    val localizations = new ApiParameter("localizations", isPart = true)
  }

}

//import analytico.macros.YTApiGenerator._
//@ytApi
//object ChannelRepresentation {
//  -'kind
//  -'etag
//  -'nextPageToken
//  -'prevPageToken
//  -'pageInfo {
//    -'totalResults
//    -'resultsPerPage
//  }
//  -'items {
//    -'kind
//    -'etag
//    +'id
//    +'snippet {
//      -'title
//      -'description
//      -'customUrl
//      -'publishedAt
//      -'thumbnails {
//        -'high {
//          -'url
//          -'width
//          -'height
//        }
//        -'medium {
//          -'url
//          -'width
//          -'height
//        }
//        -'default {
//          -'url
//          -'width
//          -'height
//        }
//      }
//      -'defaultLanguage
//      -'localized {
//        -'title
//        -'description
//      }
//      -'country
//    }
//    +'contentDetails {
//      -'relatedPlaylists {
//        -'likes
//        -'favorites
//        -'uploads
//        -'watchHistory
//        -'watchLater
//      }
//    }
//    +'statistics {
//      -'viewCount
//      -'commentCount
//      -'subscriberCount
//      -'hiddenSubscriberCount
//      -'videoCount
//    }
//    +'topicDetails {
//      -'topicIds
//      -'topicCategories
//    }
//    +'status {
//      -'privacyStatus
//      -'isLinked
//      -'longUploadsStatus
//    }
//    +'brandingSettings {
//      -'channel {
//        -'title
//        -'description
//        -'keywords
//        -'defaultTab
//        -'trackingAnalyticsAccountId
//        -'moderateComments
//        -'showRelatedChannels
//        -'showBrowseView
//        -'featuredChannelsTitle
//        -'featuredChannelsUrls
//        -'unsubscribedTrailer
//        -'profileColor
//        -'defaultLanguage
//        -'country
//      }
//      -'watch {
//        -'textColor
//        -'backgroundColor
//        -'featuredPlaylistId
//      }
//      -'image {
//        -'bannerImageUrl
//        -'bannerMobileImageUrl
//        -'watchIconImageUrl
//        -'trackingImageUrl
//        -'bannerTabletLowImageUrl
//        -'bannerTabletImageUrl
//        -'bannerTabletHdImageUrl
//        -'bannerTabletExtraHdImageUrl
//        -'bannerMobileLowImageUrl
//        -'bannerMobileMediumHdImageUrl
//        -'bannerMobileHdImageUrl
//        -'bannerMobileExtraHdImageUrl
//        -'bannerTvImageUrl
//        -'bannerTvLowImageUrl
//        -'bannerTvMediumImageUrl
//        -'bannerTvHighImageUrl
//        -'bannerExternalUrl
//      }
//      -'hints {
//        -'property
//        -'value
//      }
//    }
//    +'auditDetails {
//      -'overallGoodStanding
//      -'communityGuidelinesGoodStanding
//      -'copyrightStrikesGoodStanding
//      -'contentIdClaimsGoodStanding
//    }
//    +'contentOwnerDetails {
//      -'contentOwner
//      -'timeLinked
//    }
//    +'localizations
//  }
//}
