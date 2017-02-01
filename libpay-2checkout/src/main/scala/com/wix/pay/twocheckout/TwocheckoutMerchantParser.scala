package com.wix.pay.twocheckout

trait TwocheckoutMerchantParser {
  def parse(merchantKey: String): TwocheckoutMerchant
  def stringify(merchant: TwocheckoutMerchant): String
}
