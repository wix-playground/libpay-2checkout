package com.wix.pay.twocheckout

import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.http.{ByteArrayContent, GenericUrl, HttpRequestFactory, HttpResponseException}
import com.wix.pay.creditcard.CreditCard
import com.wix.pay.model._
import com.wix.pay.twocheckout.tokenization.TwocheckoutTokenizer
import com.wix.pay.{PaymentErrorException, PaymentGateway, PaymentRejectedException}
import org.json4s.JsonDSL._
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.util.Try

class TwocheckoutGateway(endpointUrl: String,
                         tokenizer: TwocheckoutTokenizer,
                         validator: TwocheckoutParamsValidator = TwocheckoutParamsValidator,
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
      validator.validate(creditCard, payment, customer, deal)

      val merchant = merchantParser.parse(merchantKey)
      val token = tokenizer.tokenize(merchant.sellerId, merchant.publishableKey, creditCard).get

      val url = s"$endpointUrl/checkout/api/1/${merchant.sellerId}/rs/authService"
      val requestContent = saleRequestContent(merchant, token, creditCard, payment.currencyAmount, customer.get, deal.get)

      val response = requestFactory.buildPostRequest(
        new GenericUrl(url),
        new ByteArrayContent("application/json", compact(render(requestContent)).getBytes("UTF-8"))
      ).execute()

      val responseContent2 = parse(response.getContent)
      val JString(orderNumber) = responseContent2 \\ "response" \\ "orderNumber"
      orderNumber
    }
  }

  private def saleRequestContent(merchant: TwocheckoutMerchant,
                                 token: String,
                                 creditCard: CreditCard,
                                 currencyAmount: CurrencyAmount,
                                 customer: Customer,
                                 deal: Deal) = {
    val billingAddress = creditCard.billingAddressDetailed.get
    val shippingAddress = deal.shippingAddress.get
    JObject(
      "sellerId" -> merchant.sellerId,
      "privateKey" -> merchant.privateKey,
      "token" -> token,
      "merchantOrderId" -> deal.invoiceId.get,
      "currency" -> currencyAmount.currency,
      "total" -> currencyAmount.amount,
      "billingAddr" -> JObject(
        "address1" -> billingAddress.street.get,
        "city" -> billingAddress.city.get,
        "zipCode" -> billingAddress.postalCode.get,
        "phoneNumber" -> customer.phone.get,
        "email" -> customer.email.get,
        "country" -> billingAddress.countryCode.get.getISO3Country.toUpperCase,
        "name" -> creditCard.holderName.get,
        "state" -> billingAddress.state.get
      ),
      "shippingAddr" -> JObject(
        "address1" -> shippingAddress.street.get,
        "city" -> shippingAddress.city.get,
        "zipCode" -> shippingAddress.postalCode.get,
        "country" -> shippingAddress.countryCode.get.getISO3Country.toUpperCase,
        "name" -> s"${shippingAddress.firstName.getOrElse("")} ${shippingAddress.lastName.getOrElse("")}".trim,
        "state" -> shippingAddress.state.get
      )
    )
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
    val JString(errorCode) = errorContent \\ "exception" \\ "errorCode"
    if (e.getStatusCode == 400 && errorCode.toInt >= 600) {
      // Errors with 2Checkout's custom error code above 600 -> credit card rejected
      val JString(errorMessage) = errorContent \\ "exception" \\ "errorMsg"
      throw PaymentRejectedException(errorMessage, e)
    } else {
      throw PaymentErrorException(e.getMessage, e)
    }
  }
}