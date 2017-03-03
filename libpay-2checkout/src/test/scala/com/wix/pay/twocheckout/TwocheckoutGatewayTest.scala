package com.wix.pay.twocheckout

import com.wix.pay.creditcard._
import com.wix.pay.model.{Customer, Deal, Payment}
import com.wix.pay.twocheckout.tokenization.TwocheckoutTokenizer
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecWithJUnit
import org.specs2.specification.Scope

import scala.util.Success

class TwocheckoutGatewayTest extends SpecWithJUnit with TwocheckoutTestSupport with Mockito {
  "authorize request" should {
    "throw unsupported exception" in new Ctx {
      authorize() must beUnsupportedError
    }
  }

  "capture request" should {
    "throw unsupported exception" in new Ctx {
      capture() must beUnsupportedError
    }
  }

  "sale request" should {
    "fail if payment has more than 1 installment" in new Ctx {
      sale(payment = somePayment.withInstallments(2)) must
        failWithMessage("2Checkout does not support installments!")
    }

    "fail if credit card is missing CSC" in new Ctx {
      sale(creditCard = someCreditCard.withoutCsc) must
        failWithMessage("Credit Card CSC is mandatory for 2Checkout!")
    }

    "fail for invalid merchant format" in new Ctx {
      sale(merchantKey = "invalid") must beParseError
    }

    "call tokenizer & request builder" in new Ctx {
      sale()
      there was one(tokenizer).tokenize(sellerId, publishableKey, someCreditCard)
      there was one(requestBuilder).saleRequestContent(someMerchant, token, someCreditCard, someCurrencyAmount, Some(someCustomer), Some(someDeal))
    }
  }

  "void request" should {
    "throw unsupported exception" in new Ctx {
      void() must beUnsupportedError
    }
  }

  trait Ctx extends Scope {
    val tokenizer = mock[TwocheckoutTokenizer]
    tokenizer.tokenize(sellerId, publishableKey, someCreditCard) returns Success(token)
    val requestBuilder = mock[TwocheckoutRequestBuilder]
    val gateway = new TwocheckoutGateway("", tokenizer, requestBuilder)

    def authorize() = gateway.authorize(someMerchantStr, someCreditCard, somePayment, Some(someCustomer), Some(someDeal))

    def capture() = gateway.capture(someMerchantStr, authorizationKey = "", someCurrencyAmount.amount)

    def sale(merchantKey: String = someMerchantStr,
             creditCard: CreditCard = someCreditCard,
             payment: Payment = somePayment,
             withCustomer: Option[Customer] = Some(someCustomer),
             withDeal: Option[Deal] = Some(someDeal)) =
      gateway.sale(merchantKey, creditCard, payment, withCustomer, withDeal)

    def void() = gateway.voidAuthorization(someMerchantStr, authorizationKey = "")
  }
}
