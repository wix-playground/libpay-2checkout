package com.wix.pay.twocheckout

trait TwocheckoutAuthorizationParser {
  def parse(authorizationKey: String): TwocheckoutAuthorization
  def stringify(authorization: TwocheckoutAuthorization): String
}
