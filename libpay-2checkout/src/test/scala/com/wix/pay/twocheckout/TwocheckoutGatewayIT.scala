package com.wix.pay.twocheckout

import com.wix.pay.twocheckout.testkit.TwocheckoutDriver
import com.wix.pay.twocheckout.tokenization.TwocheckoutTokenizer
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecWithJUnit
import org.specs2.specification.Scope

import scala.util.{Failure, Success}

class TwocheckoutGatewayIT extends SpecWithJUnit with TwocheckoutTestSupport with Mockito {
  val probePort = 10001
  val driver = new TwocheckoutDriver(probePort)

  val someOrderNumber = "someOrderNumber"
  val someToken = "someToken"

  step {
    driver.start()
  }

  sequential

  "sale request" should {
    "successfully yield an orderNumber upon a valid request" in new Ctx {
      givenTokenRequestReturnsToken
      givenWorldpaySaleRequest returns someOrderNumber

      sale() must beSuccessfulTry.withValue(someOrderNumber)
    }

    "fail with PaymentErrorException if token fails to acquire" in new Ctx {
      givenTokenRequestFailsWith("Some error message")

      sale() must failWithMessage("Some error message")
    }

    "fail with PaymentRejectedException for rejected transactions" in new Ctx {
      givenTokenRequestReturnsToken
      givenWorldpaySaleRequest isRejectedWith "Some error message"

      sale() must beRejectedWithMessage("Some error message")
    }

    "fail with PaymentErrorException for erroneous response" in new Ctx {
      givenTokenRequestReturnsToken
      givenWorldpaySaleRequest isAnErrorWith "Something bad happened"

      sale() must failWithMessage("Something bad happened")
    }
  }

  step {
    driver.stop()
  }

  trait Ctx extends Scope {
    val tokenizer = mock[TwocheckoutTokenizer]
    val gateway = new TwocheckoutGateway(s"http://localhost:$probePort", tokenizer)

    driver.reset()

    def givenTokenRequestReturnsToken =
      tokenizer.tokenize(sellerId, publishableKey, creditCard) returns Success(token)

    def givenTokenRequestFailsWith(errorMessage: String) =
      tokenizer.tokenize(sellerId, publishableKey, creditCard) returns Failure(new RuntimeException(errorMessage))

    def givenWorldpaySaleRequest = driver.aSaleRequest(sellerId, privateKey, token, creditCard, currencyAmount, customer, deal)
    def sale() = gateway.sale(someMerchant, creditCard, payment, Some(customer), Some(deal))
  }
}
