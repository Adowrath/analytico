package analytico

import io.circe.syntax._
import io.circe.{ Decoder, Encoder, Json }

/**
  * Circe Tests.
  */
trait CirceSpec[Parent] { self: TestSpec ⇒

  def circeTests[T <: Parent](obj: ⇒ T, jsonRepr: Json, generalName: String = "")
                             (implicit e1: Encoder[T], d1: Decoder[T],
                              e2: Encoder[Parent], d2: Decoder[Parent]): Unit = {
    it should "serialize to Json correctly" in {
      obj.asJson should ===(jsonRepr)
    }

    it should "deserialize from Json correctly" in {
      jsonRepr.as[T].right.value should ===(obj)
    }

    it should "equal itself after reserialization" in {
      obj.asJson.as[T].right.value should ===(obj)
    }

    it should "give the same Json representation after a re-serialization" in {
      jsonRepr.as[T].right.value.asJson should ===(jsonRepr)
    }

    if(generalName !== "") {
      val wrappedJson = Json.obj(
        generalName → jsonRepr
      )
      val general: Parent = obj


      it should "serialize to Json correctly if given in general form" in {
        general.asJson should ===(wrappedJson)
      }
      it should "deserialize from Json correctly if given in general form" in {
        wrappedJson.as[Parent].right.value should ===(general)
      }
      it should "equal itself after reserialization if given in general form" in {
        general.asJson.as[Parent].right.value should ===(general)
      }
      it should "give the same Json representation after a re-serialization in general form" in {
        wrappedJson.as[Parent].right.value.asJson should ===(wrappedJson)
      }
    }
  }
}
