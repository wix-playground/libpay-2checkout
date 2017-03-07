package com.wix.pay.twocheckout.tokenizer

import com.wix.pay.twocheckout.TwocheckoutTestSupport
import com.wix.pay.twocheckout.testkit.TwocheckoutDriver
import org.specs2.mutable.SpecWithJUnit
import org.specs2.specification.Scope

class HtmlTwocheckoutTokenizerIT extends SpecWithJUnit {
  val gatewayPort = 10001
  val gatewayDriver = new TwocheckoutDriver(gatewayPort)

  step {
    gatewayDriver.start()
  }

  sequential

  "HtmlTwocheckoutTokenizer" should {
    "obtain token if all requests are correct" in new Ctx {
      gatewayDriver.aPublicKeyRequest() returns somePublicKey
      gatewayDriver.aPretokenRequest(sellerId, publishableKey) returns somePretoken
      gatewayDriver.aTokenRequest(sellerId) returns token

      tokenizer.tokenize(sellerId, publishableKey, someCreditCard) must beSuccessfulTry(be_==(token))
    }

    "fail when there is no public key in response" in new Ctx {
      gatewayDriver.aPublicKeyRequest() returnsNoKey()
      tokenizer.tokenize(sellerId, publishableKey, someCreditCard) must failWithMessage("Can't load 2checkout publicKey")
    }

    "fail when pretoken request fails" in new Ctx {
      gatewayDriver.aPublicKeyRequest() returns somePublicKey
      gatewayDriver.aPretokenRequest(sellerId, publishableKey) failsWith("Some error", 502)
      tokenizer.tokenize(sellerId, publishableKey, someCreditCard) must failWithMessage("Some error")
    }

    "fail when token request fails" in new Ctx {
      gatewayDriver.aPublicKeyRequest() returns somePublicKey
      gatewayDriver.aPretokenRequest(sellerId, publishableKey) returns somePretoken
      gatewayDriver.aTokenRequest(sellerId) failsWith("Some error", 502)
      tokenizer.tokenize(sellerId, publishableKey, someCreditCard) must failWithMessage("Some error")
    }
  }

  step {
    gatewayDriver.stop()
  }

  trait Ctx extends Scope with TwocheckoutTestSupport {
    gatewayDriver.reset()

    val tokenizer = new HttpTwocheckoutTokenizer(s"http://localhost:$gatewayPort")
  }
}
