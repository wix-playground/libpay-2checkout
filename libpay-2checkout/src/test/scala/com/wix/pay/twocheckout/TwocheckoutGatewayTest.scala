package com.wix.pay.twocheckout

import com.wix.pay.creditcard._
import com.wix.pay.model.{Customer, Deal, Payment}
import com.wix.pay.twocheckout.tokenization.TwocheckoutTokenizer
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecWithJUnit
import org.specs2.specification.Scope

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
    "fail for invalid merchant format" in new Ctx {
      sale(merchantKey = "invalid") must beParseError
    }

    "validate input" in new Ctx {
      sale()
      there was one(validator).validate(creditCard, payment, Some(customer), Some(deal))
    }
  }

  "void request" should {
    "throw unsupported exception" in new Ctx {
      void() must beUnsupportedError
    }
  }

  trait Ctx extends Scope {
    val validator = mock[TwocheckoutParamsValidator]
    val gateway = new TwocheckoutGateway("", mock[TwocheckoutTokenizer], validator)

    def authorize() =
      gateway.authorize(someMerchant, creditCard, payment, Some(customer), Some(deal))

    def capture() =
      gateway.capture(someMerchant, authorizationKey = "", currencyAmount.amount)

    def sale(merchantKey: String = someMerchant,
             creditCard: CreditCard = creditCard,
             payment: Payment = payment,
             withCustomer: Option[Customer] = Some(customer),
             withDeal: Option[Deal] = Some(deal)) =
      gateway.sale(merchantKey, creditCard, payment, withCustomer, withDeal)

    def void() =
      gateway.voidAuthorization(someMerchant, authorizationKey = "")
  }
}
