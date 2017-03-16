package com.wix.pay.twocheckout.tokenizer

import com.wix.pay.creditcard.CreditCard
import org.apache.commons.codec.binary.Base64

class TokenizerRequestBuilder {

  def pretokenRequest(sellerId: String, publishableKey: String) = {
    val base64Key = Base64.encodeBase64String(publishableKey.getBytes("utf-8"))
    s"""{"sellerId":"$sellerId","publicKey":"$base64Key","userPref":""}"""
  }

  def tokenRequest(sellerId: String, paymentMethod: String) = {
    val base64Method = Base64.encodeBase64String(paymentMethod.getBytes("utf-8"))
    s"""{"sellerId":"$sellerId","paymentMethod":"$base64Method"}"""
  }

  def innerTokenRequest(card: CreditCard, publishableKey: String, preToken: String) = {
    val month = card.expiration.month formatted "%02d"
    val year = card.expiration.year
    val number = card.number
    val cvv = card.csc.get
    s"""{"paymentMethod":{"cardNum":"$number", "expMonth":"$month", "expYear":"$year", "cvv":"$cvv", "cardType":"CC"},
       |"pubAccessKey":"$publishableKey",
       |"preToken":"$preToken"}""".stripMargin
  }

}
