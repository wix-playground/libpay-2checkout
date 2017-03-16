package com.wix.pay.twocheckout.tokenizer

import com.wix.pay.PaymentErrorException
import com.wix.pay.testkit.LibPayTestSupport
import com.wix.pay.twocheckout.model.{TwocheckoutEnvironment, TwocheckoutSettings}
import com.wix.pay.twocheckout.testkit.TwocheckoutDriver
import com.wix.pay.twocheckout.tokenization.RSAPublicKey
import org.specs2.matcher.Matcher
import org.specs2.mutable.SpecWithJUnit
import org.specs2.specification.Scope

import scala.util.Try

class HttpTwocheckoutTokenizerIT extends SpecWithJUnit {
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

      tokenizer.tokenize(sellerId, publishableKey, someCreditCard, false) must beSuccessfulTry(be_==(token))
    }

    "fail when there is no public key in response" in new Ctx {
      gatewayDriver.aPublicKeyRequest() returnsNoKey()
      tokenizer.tokenize(sellerId, publishableKey, someCreditCard, false) must failWithMessage("Can't load 2checkout publicKey")
    }

    "fail when pretoken request fails" in new Ctx {
      gatewayDriver.aPublicKeyRequest() returns somePublicKey
      gatewayDriver.aPretokenRequest(sellerId, publishableKey) failsWith("Some error", 502)
      tokenizer.tokenize(sellerId, publishableKey, someCreditCard, false) must failWithMessage("Some error")
    }

    "fail when token request fails" in new Ctx {
      gatewayDriver.aPublicKeyRequest() returns somePublicKey
      gatewayDriver.aPretokenRequest(sellerId, publishableKey) returns somePretoken
      gatewayDriver.aTokenRequest(sellerId) failsWith("Some error", 502)
      tokenizer.tokenize(sellerId, publishableKey, someCreditCard, false) must failWithMessage("Some error")
    }
  }

  step {
    gatewayDriver.stop()
  }

  trait Ctx extends Scope with LibPayTestSupport {
    val sellerId = "someSellerId"
    val publishableKey = "somePublishableKey"
    val somePretoken = "somePretoken"
    val token = "someToken"
    val somePublicKey = RSAPublicKey(
      "AMroNi0ZH7gGJPzgZP11kwEl++ZZgmQeQpqD69Ghgp72cPMNDDe217HzPrULQEUBQwyX21i1ZagHU9jJTSbHMwtoZRCCa8AiWvxBtO1XJ7" +
        "4nU9heeQScyf3M25Lu9wxPKVfaTrMcXi879TjZm8TNqr89jBqCF1NUtDO+EFFi4OStKf9ILd0DMBYBhOdxBkBmBSy8VIhw0n0JI6RhSERv" +
        "LI6Ia7n63VEOCC8zfdTUwmp2e4g7M0DHvOPqZ9Ldoy4g5DQqQZW/qRVYgKgxlOXUBnJD7HquMg1oWWrYL0zWmBBEG/aOOOpgxqrCM7fmml" +
        "0A4dKqS4blxeT99p7Tori9VBM=",
      "AQAB"
    )

    gatewayDriver.reset()

    val environment = TwocheckoutEnvironment(s"http://localhost:$gatewayPort", s"http://localhost:$gatewayPort")
    val tokenizer = new HttpTwocheckoutTokenizer(TwocheckoutSettings(environment))

    def failWithMessage(message: String): Matcher[Try[String]] = beFailedTry.like { case e: PaymentErrorException => e.message must contain(message) }
  }
}
