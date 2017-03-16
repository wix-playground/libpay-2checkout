package com.wix.pay.twocheckout

import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.http.{ByteArrayContent, GenericUrl, HttpRequestFactory, HttpResponseException}
import com.wix.pay.creditcard.CreditCard
import com.wix.pay.model._
import com.wix.pay.twocheckout.model.TwocheckoutSettings
import com.wix.pay.twocheckout.tokenization.TwocheckoutTokenizer
import com.wix.pay.{PaymentErrorException, PaymentGateway, PaymentRejectedException}
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.util.Try

class TwocheckoutGateway(settings: TwocheckoutSettings,
                         tokenizer: TwocheckoutTokenizer,
                         requestBuilder: TwocheckoutRequestBuilder = new TwocheckoutRequestBuilder(),
                         merchantParser: TwocheckoutMerchantParser = JsonTwocheckoutMerchantParser) extends PaymentGateway {

  private val requestFactory: HttpRequestFactory = new NetHttpTransport().createRequestFactory()

  override def authorize(merchantKey: String, creditCard: CreditCard, payment: Payment, customer: Option[Customer], deal: Option[Deal]): Try[String] = {
    withExceptionHandling {
      throw new UnsupportedOperationException("authorize is not supported for 2Checkout!")
    }
  }

  override def capture(merchantKey: String, authorizationKey: String, amount: Double): Try[String] = {
    withExceptionHandling {
      throw new UnsupportedOperationException("capture is not supported for 2Checkout!")
    }
  }

  override def sale(merchantKey: String, creditCard: CreditCard, payment: Payment, customer: Option[Customer], deal: Option[Deal]): Try[String] = {
    withExceptionHandling {
      validateParams(payment, creditCard)

      val merchant = merchantParser.parse(merchantKey)
      val token = tokenizer.tokenize(merchant.sellerId, merchant.publishableKey, creditCard, merchant.sandboxMode).get

      val requestContent = requestBuilder.saleRequestContent(merchant, token, creditCard, payment.currencyAmount, customer, deal)

      val endpointUrl = settings.endpointUrl(merchant.sandboxMode)
      val response = requestFactory.buildPostRequest(
        new GenericUrl(s"$endpointUrl/checkout/api/1/${merchant.sellerId}/rs/authService"),
        new ByteArrayContent("application/json", compact(render(requestContent)).getBytes("UTF-8"))
      ).execute()

      try {
        val responseContent = parse(response.getContent)
        val JString(orderNumber) = responseContent \ "response" \ "orderNumber"
        orderNumber
      } finally {
        response.disconnect()
      }
    }
  }

  private def validateParams(payment: Payment, creditCard: CreditCard): Unit = {
    require(payment.installments == 1, "2Checkout does not support installments!")
    require(creditCard.csc.isDefined, "Credit Card CSC is mandatory for 2Checkout!")
  }

  override def voidAuthorization(merchantKey: String, authorizationKey: String): Try[String] = {
    withExceptionHandling {
      throw new UnsupportedOperationException("voidAuthorization is not supported for 2Checkout!")
    }
  }

  private def withExceptionHandling[T](f: => T): Try[T] = {
    Try {
      f
    } recover {
      case e: PaymentRejectedException => throw e
      case e: HttpResponseException => throw handleHttpException(e)
      case e => throw PaymentErrorException(e.getMessage, e)
    }
  }

  private def handleHttpException(e: HttpResponseException): Nothing = {
    val errorContent = parse(e.getContent)
    val JString(errorCode) = errorContent \ "exception" \ "errorCode"
    if (e.getStatusCode == 400 && errorCode.toInt >= 600) {
      // Errors with 2Checkout's custom error code above 600 -> credit card rejected
      val JString(errorMessage) = errorContent \ "exception" \ "errorMsg"
      throw PaymentRejectedException(errorMessage, e)
    } else {
      throw PaymentErrorException(e.getMessage, e)
    }
  }
}