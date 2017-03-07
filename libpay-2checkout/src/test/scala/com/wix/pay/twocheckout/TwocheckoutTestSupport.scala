package com.wix.pay.twocheckout

import com.wix.pay.testkit.LibPayTestSupport
import com.wix.pay.twocheckout.tokenization.RSAPublicKey
import com.wix.pay.{PaymentErrorException, PaymentRejectedException}
import org.json4s.ParserUtil.ParseException
import org.specs2.matcher.Matcher
import org.specs2.matcher.MustThrownMatchers._

import scala.reflect.ClassTag
import scala.util.Try

trait TwocheckoutTestSupport extends LibPayTestSupport {
  val sellerId = "someSellerId"
  val publishableKey = "somePublishableKey"
  val privateKey = "somePrivateKey"
  val somePretoken = "somePretoken"
  val token = "someToken"
  val someMerchant = TwocheckoutMerchant(sellerId, publishableKey, privateKey)
  val someMerchantStr = JsonTwocheckoutMerchantParser.stringify(someMerchant)

  val somePublicKey = RSAPublicKey(
    "AMroNi0ZH7gGJPzgZP11kwEl++ZZgmQeQpqD69Ghgp72cPMNDDe217HzPrULQEUBQwyX21i1ZagHU9jJTSbHMwtoZRCCa8AiWvxBtO1XJ7" +
      "4nU9heeQScyf3M25Lu9wxPKVfaTrMcXi879TjZm8TNqr89jBqCF1NUtDO+EFFi4OStKf9ILd0DMBYBhOdxBkBmBSy8VIhw0n0JI6RhSERv" +
      "LI6Ia7n63VEOCC8zfdTUwmp2e4g7M0DHvOPqZ9Ldoy4g5DQqQZW/qRVYgKgxlOXUBnJD7HquMg1oWWrYL0zWmBBEG/aOOOpgxqrCM7fmml" +
      "0A4dKqS4blxeT99p7Tori9VBM=",
    "AQAB"
  )

  def beRejectedWithMessage(message: String): Matcher[Try[String]] = beFailedTry.like { case e: PaymentRejectedException => e.message mustEqual message }
  def failWithMessage(message: String): Matcher[Try[String]] = beFailedTry.like { case e: PaymentErrorException => e.message must contain(message) }

  def beParseError: Matcher[Try[String]] = failWithCause[ParseException]
  def beUnsupportedError: Matcher[Try[String]] = failWithCause[UnsupportedOperationException]
  private def failWithCause[T : ClassTag]: Matcher[Try[String]] = beFailedTry.like { case e: PaymentErrorException => e.cause must beAnInstanceOf[T] }
}
