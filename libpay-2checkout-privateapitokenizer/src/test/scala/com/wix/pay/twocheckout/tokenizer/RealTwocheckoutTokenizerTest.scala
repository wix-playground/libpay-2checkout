package com.wix.pay.twocheckout.tokenizer

import com.wix.pay.PaymentErrorException
import com.wix.pay.creditcard.{CreditCard, CreditCardOptionalFields, YearMonth}
import com.wix.pay.twocheckout.model.{TwocheckoutEnvironment, TwocheckoutSettings}
import org.specs2.mutable.SpecWithJUnit
import org.specs2.specification.Scope

class RealTwocheckoutTokenizerTest extends SpecWithJUnit {
  skipAll

  "HttpTwocheckoutTokenizer" should {
    "obtain token on valid credentials" in new Ctx {
      tokenizer.tokenize(sellerId, publishableKey, testCreditCard, true) must beSuccessfulTry[String]
    }

    "fail on invalid credentials" in new Ctx {
      tokenizer.tokenize(wrongSellerId, publishableKey, testCreditCard, true) must beFailedTry(beAnInstanceOf[PaymentErrorException])
    }
  }

  trait Ctx extends Scope {
    val settings = TwocheckoutSettings(
      production = TwocheckoutEnvironment("https://www.2checkout.com", "https://www.2checkout.com/checkout/api/2co.min.js"),
      sandbox = TwocheckoutEnvironment("https://sandbox.2checkout.com", "https://sandbox.2checkout.com/checkout/api/2co.min.js")
    )
    val tokenizer = new HttpTwocheckoutTokenizer(settings)

    /**
      * username: 2checkout-Test
      * email: 2checkout-Test@mailinator.com
      * password: 2checkout-Test@mailinator.com
      */
    val sellerId = "901338726"
    val publishableKey = "19CFABDB-BA94-45B8-935A-E3A1B2469F1F"

    val wrongSellerId = "11111111"

    val testCreditCard = CreditCard("4000000000000002", YearMonth(2019, 1),
      Some(CreditCardOptionalFields.withFields(csc = Some("471"), holderName = Some("John Doe"))))
  }
}
