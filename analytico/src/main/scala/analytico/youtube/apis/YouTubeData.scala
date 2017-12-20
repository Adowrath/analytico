package analytico
package youtube
package apis

import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.ChannelListResponse

class YouTubeData[S <: YTScope](apiAccess: YouTube) {
  def channels: ChannelView = ChannelView()

  case class ChannelView(isMine: Boolean = false) {
    def mine: ChannelView = copy(isMine = true)

    def list(listItems: (ChannelRepresentation.type ⇒ ApiParameter)*): ChannelListResponse = {

      val parameters = listItems map {
        _ (ChannelRepresentation)
      }
      val fields = parameters map {
        _.repr
      } mkString ", "
      val parts = parameters flatMap {
        _.getParts
      } map {
        _.repr
      } mkString ","

      val channelRequest = apiAccess.channels.list(parts)
      channelRequest.setMine(isMine)
      channelRequest.setFields(fields)
      val channels = channelRequest.execute

      channels
    }
  }

}
