package com.wix.pay.twocheckout

import com.wix.pay.twocheckout.testkit.{TwocheckoutDriver, TwocheckoutJavascriptSdkDriver}
import com.wix.pay.twocheckout.tokenizer.HttpTwocheckoutTokenizer
import org.specs2.mutable.SpecWithJUnit
import org.specs2.specification.Scope

class TwocheckoutGatewayIT extends SpecWithJUnit with TwocheckoutTestSupport {
  val gatewayPort = 10002
  val gatewayDriver = new TwocheckoutDriver(gatewayPort)

  val someOrderNumber = "someOrderNumber"
  val someToken = "someToken"

  step {
    gatewayDriver.start()
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
    gatewayDriver.stop()
  }

  trait Ctx extends Scope {
    private val tokenizer = new HttpTwocheckoutTokenizer(s"http://localhost:$gatewayPort")
    val gateway = new TwocheckoutGateway(s"http://localhost:$gatewayPort", tokenizer)

    gatewayDriver.reset()

    def givenTokenRequestReturnsToken = {
      gatewayDriver.aPublicKeyRequest() returns somePublicKey
      gatewayDriver.aPretokenRequest(sellerId, publishableKey) returns somePretoken
      gatewayDriver.aTokenRequest(sellerId) returns token
    }

    def givenTokenRequestFailsWith(errorMessage: String) = {
      gatewayDriver.aPublicKeyRequest() returns somePublicKey
      gatewayDriver.aPretokenRequest(sellerId, publishableKey) failsWith(errorMessage, 500)
    }

    def givenWorldpaySaleRequest = gatewayDriver.aSaleRequest(sellerId, privateKey, token, someCreditCard, someCurrencyAmount, Some(someCustomer), Some(someDeal))
    def sale() = gateway.sale(someMerchantStr, someCreditCard, somePayment, Some(someCustomer), Some(someDeal))
  }
}
