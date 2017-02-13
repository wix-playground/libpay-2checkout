package com.wix.pay.twocheckout

import org.json4s.DefaultFormats
import org.json4s.native.Serialization

object JsonTwocheckoutMerchantParser extends TwocheckoutMerchantParser {
  private implicit val formats = DefaultFormats

  override def parse(merchantKey: String): TwocheckoutMerchant = {
    Serialization.read[TwocheckoutMerchant](merchantKey)
  }

  override def stringify(merchant: TwocheckoutMerchant): String = {
    Serialization.write(merchant)
  }
}
