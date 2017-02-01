package com.wix.pay.twocheckout.tokenization.html

import com.wix.pay.creditcard.{CreditCard, CreditCardOptionalFields, YearMonth}
import com.wix.pay.twocheckout.model.Environments
import com.wix.pay.twocheckout.tokenization.html.model.ErrorCodes
import org.specs2.mutable.SpecWithJUnit
import org.specs2.specification.Scope


/**
  * @note This test interacts with 2checkout's sandbox servers.
  */
class HtmlTokenizerTest extends SpecWithJUnit {
  /**
    * username: 2checkout-Test
    * email: 2checkout-Test@mailinator.com
    * password: 2checkout-Test@mailinator.com
    */
  val sellerId = "901338726"
  val publishableKey = "19CFABDB-BA94-45B8-935A-E3A1B2469F1F"

  val card = CreditCard(
    number = "4222222222222",
    expiration = YearMonth(
      year = 2020,
      month = 12
    ),
    additionalFields = Some(CreditCardOptionalFields.withFields(
      csc = Some("123")
    ))
  )

  val tokenizer = new HtmlTokenizer(
    environment = Environments.sandbox
  )

  trait Ctx extends Scope {}

  "tokenize" should {
    "tokenize cards" in new Ctx {
      tokenizer.tokenize(
        sellerId = sellerId,
        publishableKey = publishableKey,
        card = card
      ) must beASuccessfulTry(
        check = not(beEmpty[String])
      )
    }

    "gracefully fail on invalid merchant information" in new Ctx {
      tokenizer.tokenize(
        sellerId = "123",
        publishableKey = "invalid-key",
        card = card
      ) must beAFailedTry.like {
        case e: Throwable => e.getMessage must contain(ErrorCodes.unauthorized)
      }
    }
  }
}