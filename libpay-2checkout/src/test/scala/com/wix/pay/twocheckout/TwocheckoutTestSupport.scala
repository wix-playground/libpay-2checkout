package com.wix.pay.twocheckout

import com.wix.pay.testkit.LibPayTestSupport
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
  val token = "someToken"
  val someMerchant = TwocheckoutMerchant(sellerId, publishableKey, privateKey)
  val someMerchantStr = JsonTwocheckoutMerchantParser.stringify(someMerchant)

  def beRejectedWithMessage(message: String): Matcher[Try[String]] = beFailedTry.like { case e: PaymentRejectedException => e.message mustEqual message }
  def failWithMessage(message: String): Matcher[Try[String]] = beFailedTry.like { case e: PaymentErrorException => e.message must contain(message) }

  def beParseError: Matcher[Try[String]] = failWithCause[ParseException]
  def beUnsupportedError: Matcher[Try[String]] = failWithCause[UnsupportedOperationException]
  private def failWithCause[T : ClassTag]: Matcher[Try[String]] = beFailedTry.like { case e: PaymentErrorException => e.cause must beAnInstanceOf[T] }
}
