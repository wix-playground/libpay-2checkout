package com.wix.pay.twocheckout

import com.wix.pay.twocheckout.model.Environments
import com.wix.pay.twocheckout.model.html.Error
import com.wix.pay.twocheckout.testkit.{TwocheckoutDriver, TwocheckoutJavascriptSdkDriver}
import com.wix.pay.twocheckout.tokenization.html.HtmlTokenizer
import org.specs2.mutable.SpecWithJUnit
import org.specs2.specification.Scope

class TwocheckoutGatewayIT extends SpecWithJUnit with TwocheckoutTestSupport {
  val gatewayPort = 10001
  val jsPort = 10002
  val gatewayDriver = new TwocheckoutDriver(gatewayPort)
  val jsDriver = new TwocheckoutJavascriptSdkDriver(jsPort)

  val someOrderNumber = "someOrderNumber"
  val someToken = "someToken"

  step {
    gatewayDriver.start()
    jsDriver.start()
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
    jsDriver.stop()
  }

  trait Ctx extends Scope {
    private val tokenizer = new HtmlTokenizer(jsSdkUrl = s"http://localhost:$jsPort")
    val gateway = new TwocheckoutGateway(s"http://localhost:$gatewayPort", tokenizer)

    gatewayDriver.reset()

    def givenTokenRequestReturnsToken = jsDriver.aJavascriptSdkRequest(
      sellerId = sellerId,
      publishableKey = publishableKey,
      environment = Environments.production,
      creditCard = creditCard
    ).successfullyTokenizes(
      token = token
    )

    def givenTokenRequestFailsWith(errorMessage: String) = jsDriver.aJavascriptSdkRequest(
      sellerId = sellerId,
      publishableKey = publishableKey,
      environment = Environments.production,
      creditCard = creditCard
    ).failsTokenizing(
      error = Error(errorCode = "300", errorMsg = errorMessage)
    )

    def givenWorldpaySaleRequest = gatewayDriver.aSaleRequest(sellerId, privateKey, token, creditCard, currencyAmount, Some(customer), Some(deal))
    def sale() = gateway.sale(someMerchantStr, creditCard, payment, Some(customer), Some(deal))
  }
}
