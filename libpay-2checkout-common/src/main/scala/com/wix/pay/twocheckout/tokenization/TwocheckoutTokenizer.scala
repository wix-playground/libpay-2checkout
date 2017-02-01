package com.wix.pay.twocheckout.tokenization

import com.wix.pay.creditcard.CreditCard

import scala.util.Try

trait TwocheckoutTokenizer {
  def tokenize(sellerId: String, publishableKey: String, card: CreditCard): Try[String]
}
