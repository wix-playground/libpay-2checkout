package com.wix.pay.twocheckout.tokenization.html

import com.wix.pay.creditcard.{CreditCard, CreditCardOptionalFields, YearMonth}
import com.wix.pay.twocheckout.model.html.{Error, ErrorCodes}
import com.wix.pay.twocheckout.testkit.TwocheckoutJavascriptSdkDriver
import org.specs2.mutable.SpecWithJUnit
import org.specs2.specification.Scope


/**
  * @note This test interacts with 2checkout's sandbox servers.
  */
class HtmlTokenizerTest extends SpecWithJUnit {
  val driverPort = 10005
  val driver = new TwocheckoutJavascriptSdkDriver(driverPort)

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

  val someEnvironment = "some-environment"
  val someToken = "some-token"

  val tokenizer = new HtmlTokenizer(
    jsSdkUrl = s"http://localhost:$driverPort/",
    environment = someEnvironment
  )

  trait Ctx extends Scope {
    driver.reset()
  }

  step {
    driver.start()
  }

  sequential

  "tokenize" should {
    "tokenize cards" in new Ctx {
      driver.aJavascriptSdkRequest().successfullyTokenizes(
        sellerId = sellerId,
        publishableKey = publishableKey,
        ccNo = card.number,
        cvv = card.csc.get,
        expMonth = card.expiration.month,
        expYear = card.expiration.year,
        environment = someEnvironment,
        token = someToken
      )

      tokenizer.tokenize(
        sellerId = sellerId,
        publishableKey = publishableKey,
        card = card
      ) must beASuccessfulTry(
        check = ===(someToken)
      )
    }

    "gracefully fail on invalid merchant information" in new Ctx {
      val someErrorMessage = "some error message"
      driver.aJavascriptSdkRequest().failsTokenizing(
        sellerId = sellerId,
        publishableKey = publishableKey,
        ccNo = card.number,
        cvv = card.csc.get,
        expMonth = card.expiration.month,
        expYear = card.expiration.year,
        environment = someEnvironment,
        error = Error(
          errorCode = ErrorCodes.unauthorized,
          errorMsg = someErrorMessage
        )
      )

      tokenizer.tokenize(
        sellerId = sellerId,
        publishableKey = publishableKey,
        card = card
      ) must beAFailedTry.like {
        case e: Throwable => e.getMessage must (contain(ErrorCodes.unauthorized) and contain(someErrorMessage))
      }
    }
  }

  step {
    driver.stop()
  }
}