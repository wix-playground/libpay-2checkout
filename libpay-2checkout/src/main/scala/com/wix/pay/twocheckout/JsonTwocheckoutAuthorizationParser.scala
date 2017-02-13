package com.wix.pay.twocheckout

import org.json4s.DefaultFormats
import org.json4s.native.Serialization

object JsonTwocheckoutAuthorizationParser extends TwocheckoutAuthorizationParser {
  private implicit val formats = DefaultFormats

  override def parse(authorizationKey: String): TwocheckoutAuthorization = {
    Serialization.read[TwocheckoutAuthorization](authorizationKey)
  }

  override def stringify(authorization: TwocheckoutAuthorization): String = {
    Serialization.write(authorization)
  }
}
