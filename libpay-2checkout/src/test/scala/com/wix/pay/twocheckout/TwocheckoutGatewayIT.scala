package com.wix.pay.twocheckout


import com.wix.pay.PaymentRejectedException

import scala.util.Try
import org.specs2.mutable.SpecWithJUnit
import org.specs2.specification.Scope
import com.wix.pay.twocheckout.model.{TwocheckoutEnvironment, TwocheckoutSettings}
import com.wix.pay.twocheckout.testkit.TwocheckoutDriver
import com.wix.pay.twocheckout.tokenizer.HttpTwocheckoutTokenizer


class TwocheckoutGatewayIT extends SpecWithJUnit with TwocheckoutTestSupport {
  val gatewayPort = 10002
  val gatewayDriver = new TwocheckoutDriver(gatewayPort)

  val someOrderNumber = "someOrderNumber"
  val someToken = "someToken"

  private val settings = TwocheckoutSettings(TwocheckoutEnvironment(
    s"http://localhost:$gatewayPort", s"http://localhost:$gatewayPort"))
  private val tokenizer = new HttpTwocheckoutTokenizer(settings)
  val gateway = new TwocheckoutGateway(settings, tokenizer)

  def givenTokenRequestReturnsToken(): Unit = {
    gatewayDriver.aPublicKeyRequest() returns somePublicKey
    gatewayDriver.aPretokenRequest(sellerId, publishableKey) returns somePretoken
    gatewayDriver.aTokenRequest(sellerId) returns token
  }

  def givenTokenRequestFailsWith(errorMessage: String): Unit = {
    gatewayDriver.aPublicKeyRequest() returns somePublicKey
    gatewayDriver.aPretokenRequest(sellerId, publishableKey) failsWith(errorMessage, 500)
  }

  def givenWorldpaySaleRequest: gatewayDriver.SaleRequest = {
    gatewayDriver.aSaleRequest(
      sellerId,
      privateKey,
      token,
      someCreditCard,
      someCurrencyAmount,
      Some(someCustomer),
      Some(someDeal))
  }

  def sale(): Try[String] = {
    gateway.sale(
      someMerchantStr,
      someCreditCard,
      somePayment,
      Some(someCustomer),
      Some(someDeal))
  }


  step {
    gatewayDriver.start()
  }


  sequential


  "sale request" should {
    "successfully yield an orderNumber upon a valid request" in new Ctx {
      givenTokenRequestReturnsToken()
      givenWorldpaySaleRequest returns someOrderNumber

      sale() must beSuccessfulTry.withValue(someOrderNumber)
    }

    "fail with PaymentErrorException if token fails to acquire" in new Ctx {
      givenTokenRequestFailsWith("Some error message")

      sale() must failWithMessage("Some error message")
    }

    "fail with PaymentRejectedException for rejected transactions" in new Ctx {
      givenTokenRequestReturnsToken()
      givenWorldpaySaleRequest getsRejectedWith "Some error message"

      sale() must beRejectedWithMessage("Some error message")
    }


    "fail with PaymentRejectedException with transactionId of type string for rejected transactions" in new Ctx {
      givenTokenRequestReturnsToken()
      val someTransactionId = "someTransactionId"
      givenWorldpaySaleRequest getsRejectedWith("Some error message", Some(someTransactionId))

      sale() must {
        beRejectedWithMessage("Some error message") and
        beRejectedWithTransactionId(Some(someTransactionId))
      }
    }

    "fail with PaymentRejectedException with transactionId of type Integer for rejected transactions" in new Ctx {
      givenTokenRequestReturnsToken()
      val someTransactionId = 123455
      givenWorldpaySaleRequest getsRejectedWith("Some error message", Some(someTransactionId))

      sale() must {
        beRejectedWithMessage("Some error message") and
          beRejectedWithTransactionId(Some(someTransactionId.toString))
      }
    }

    "fail with PaymentErrorException for erroneous response" in new Ctx {
      givenTokenRequestReturnsToken()
      givenWorldpaySaleRequest getsAnErrorWith "Something bad happened"

      sale() must failWithMessage("Something bad happened")
    }
  }


  step {
    gatewayDriver.stop()
  }


  trait Ctx extends Scope {
    gatewayDriver.reset()
  }
}
