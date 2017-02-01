package com.wix.pay.twocheckout

import com.wix.pay.PaymentGateway
import com.wix.pay.creditcard.CreditCard
import com.wix.pay.model.{CurrencyAmount, Customer, Deal}
import com.wix.pay.twocheckout.model.Environments
import com.wix.pay.twocheckout.tokenization.TwocheckoutTokenizer

import scala.util.Try

class TwocheckoutGateway(merchantParser: TwocheckoutMerchantParser = new JsonTwocheckoutMerchantParser,
                         tokenizer: TwocheckoutTokenizer,
                         environment: String = Environments.production) extends PaymentGateway {

  override def authorize(merchantKey: String, creditCard: CreditCard, currencyAmount: CurrencyAmount, customer: Option[Customer], deal: Option[Deal]): Try[String] = {
    ???
  }

  override def capture(merchantKey: String, authorizationKey: String, amount: Double): Try[String] = {
    ???
  }

  override def sale(merchantKey: String, creditCard: CreditCard, currencyAmount: CurrencyAmount, customer: Option[Customer], deal: Option[Deal]): Try[String] = {
    ???
  }

  override def voidAuthorization(merchantKey: String, authorizationKey: String): Try[String] = {
    ???
  }
}
