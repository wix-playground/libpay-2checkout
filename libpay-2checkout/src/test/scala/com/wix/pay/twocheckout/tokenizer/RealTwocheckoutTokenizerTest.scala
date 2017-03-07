package com.wix.pay.twocheckout.tokenizer

import com.wix.pay.PaymentErrorException
import com.wix.pay.creditcard.{CreditCard, CreditCardOptionalFields, YearMonth}
import org.specs2.mutable.SpecWithJUnit
import org.specs2.specification.Scope

class RealTwocheckoutTokenizerTest extends SpecWithJUnit {
  skipAll

  "HttpTwocheckoutTokenizer" should {
    "obtain token on valid credentials" in new Ctx {
      tokenizer.tokenize(sellerId, publishableKey, testCreditCard) must beSuccessfulTry[String]
    }

    "fail on invalid credentials" in new Ctx {
      tokenizer.tokenize(wrongSellerId, publishableKey, testCreditCard) must beFailedTry(beAnInstanceOf[PaymentErrorException])
    }
  }

  trait Ctx extends Scope {
    val tokenizer = new HttpTwocheckoutTokenizer("https://sandbox.2checkout.com")

    val sellerId = "yourSellerId"
    val publishableKey = "yourPublishableKey"

    val wrongSellerId = "11111111"

    val testCreditCard = CreditCard("4000000000000002", YearMonth(2019, 1),
      Some(CreditCardOptionalFields.withFields(csc = Some("471"), holderName = Some("John Doe"))))
  }
}
