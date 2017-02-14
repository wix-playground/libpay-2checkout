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
      sale(payment = payment.withInstallments(2)) must
        failWithMessage("2Checkout does not support installments!")
    }

    "fail if credit card is missing CSC" in new Ctx {
      sale(creditCard = creditCard.withoutCsc) must
        failWithMessage("Credit Card CSC is mandatory for 2Checkout!")
    }

    "fail for invalid merchant format" in new Ctx {
      sale(merchantKey = "invalid") must beParseError
    }

    "call tokenizer & request builder" in new Ctx {
      sale()
      there was one(tokenizer).tokenize(sellerId, publishableKey, creditCard)
      there was one(requestBuilder).saleRequestContent(someMerchant, token, creditCard, currencyAmount, Some(customer), Some(deal))
    }
  }

  "void request" should {
    "throw unsupported exception" in new Ctx {
      void() must beUnsupportedError
    }
  }

  trait Ctx extends Scope {
    val tokenizer = mock[TwocheckoutTokenizer]
    tokenizer.tokenize(sellerId, publishableKey, creditCard) returns Success(token)
    val requestBuilder = mock[TwocheckoutRequestBuilder]
    val gateway = new TwocheckoutGateway("", tokenizer, requestBuilder)

    def authorize() = gateway.authorize(someMerchantStr, creditCard, payment, Some(customer), Some(deal))

    def capture() = gateway.capture(someMerchantStr, authorizationKey = "", currencyAmount.amount)

    def sale(merchantKey: String = someMerchantStr,
             creditCard: CreditCard = creditCard,
             payment: Payment = payment,
             withCustomer: Option[Customer] = Some(customer),
             withDeal: Option[Deal] = Some(deal)) =
      gateway.sale(merchantKey, creditCard, payment, withCustomer, withDeal)

    def void() = gateway.voidAuthorization(someMerchantStr, authorizationKey = "")
  }
}
