package com.wix.pay.twocheckout.testkit

import com.wix.hoopoe.http.testkit.EmbeddedHttpProbe
import com.wix.pay.creditcard.CreditCard
import com.wix.pay.model.{CurrencyAmount, Customer, Deal}
import org.json4s.DefaultFormats
import org.json4s.native.Serialization
import spray.http._

class TwocheckoutDriver(port: Int) {
  private val probe = new EmbeddedHttpProbe(port, EmbeddedHttpProbe.NotFoundHandler)

  def reset(): Unit = probe.reset()

  def start(): Unit = probe.doStart()

  def stop(): Unit = probe.doStop()

  def aSaleRequest(sellerId: String,
                   privateKey: String,
                   token: String,
                   creditCard: CreditCard,
                   currencyAmount: CurrencyAmount,
                   customer: Customer,
                   deal: Deal) = {
    SaleRequest(sellerId, privateKey, token, creditCard, currencyAmount, customer, deal)
  }

  case class SaleRequest(sellerId: String,
                         privateKey: String,
                         token: String,
                         creditCard: CreditCard,
                         currencyAmount: CurrencyAmount,
                         customer: Customer,
                         deal: Deal) {

    def returns(orderNumber: String): Unit = {
      val response = removeEmptyValuesFromMap(validResponse(orderNumber))
      respondWith(StatusCodes.OK, toJson(response))
    }

    def isRejectedWith(message: String): Unit = respondWith(StatusCodes.BadRequest, toJson(rejected(message)))
    def isAnErrorWith(message: String): Unit = respondWith(StatusCodes.InternalServerError, toJson(internalError(message)))

    private def rejected(message: String) = errorResponse(message, httpStatus = 400, errorCode = 607)
    private def internalError(message: String) = errorResponse(message, httpStatus = 500, errorCode = 400)

    private def errorResponse(errorDescription: String, httpStatus: Int, errorCode: Int) = Map(
      "validationErrors" -> null,
      "response" -> null,
      "exception" -> Map(
        "errorMsg" -> errorDescription,
        "httpStatus" -> httpStatus.toString,
        "exception" -> false,
        "errorCode" -> errorCode.toString
      )
    )

    private def respondWith(status: StatusCode, content: String): Unit = {
      probe.handlers += {
        case HttpRequest(HttpMethods.POST, requestPath, headers, entity, _)
          if requestPath.path == Uri(s"https://sandbox.2checkout.com/checkout/api/1/$sellerId/rs/authService").path &&
            isJson(headers) &&
            isStubbedEntity(entity) =>
          HttpResponse(status = status, entity = content)
      }
    }

    private def isJson(headers: List[HttpHeader]): Boolean = headers.exists { header =>
      header.name == "Content-Type" && header.value == "application/json"
    }

    private def isStubbedEntity(entity: HttpEntity): Boolean = {
      val actual = toMap(entity)
      val expected = removeEmptyValuesFromMap(expectedJsonBody)
      actual == expected
    }

    private def expectedJsonBody = Map(
      "sellerId" -> sellerId,
      "privateKey" -> privateKey,
      "token" -> token,
      "merchantOrderId" -> deal.invoiceId.get,
      "currency" -> currencyAmount.currency,
      "total" -> currencyAmount.amount,
      "billingAddr" -> requestBillingAddressMap,
      "shippingAddr" -> requestShippingAddressMap
    )

    private val billingAddress = creditCard.billingAddressDetailed.get
    private val requestBillingAddressMap = Map(
      "address1" -> billingAddress.street.get,
      "city" -> billingAddress.city.get,
      "zipCode" -> billingAddress.postalCode.get,
      "phoneNumber" -> customer.phone.get,
      "email" -> customer.email.get,
      "country" -> billingAddress.countryCode.get.getISO3Country.toUpperCase,
      "name" -> creditCard.holderName.get,
      "state" -> billingAddress.state.get
    )
    private val responseBillingAddressMap = requestBillingAddressMap ++ Map(
      "addrLine2" -> null,
      "phoneExtension" -> null
    )

    private val shippingAddress = deal.shippingAddress.get
    private val requestShippingAddressMap = Map(
      "address1" -> shippingAddress.street,
      "city" -> shippingAddress.city,
      "zipCode" -> shippingAddress.postalCode,
      "country" -> shippingAddress.countryCode.map(_.getISO3Country.toUpperCase),
      "name" -> s"${shippingAddress.firstName.getOrElse("")} ${shippingAddress.lastName.getOrElse("")}".trim,
      "state" -> shippingAddress.state
    )
    private val responseShippingAddressMap = requestShippingAddressMap ++ Map(
      "addrLine2" -> null,
      "phoneNumber" -> null,
      "phoneExtension" -> null,
      "email" -> null
    )

    implicit val formats = DefaultFormats
    private def toJson(map: Map[String, Any]): String = Serialization.write(map)
    private def toMap(entity: HttpEntity): Map[String, Any] = Serialization.read[Map[String, Any]](entity.asString)

    private def removeEmptyValuesFromMap(map: Map[String, Any]): Map[String, Any] = map.flatMap { case (key, value) =>
      val filteredValue = filterValue(value)
      filteredValue.map(key -> _)
    }

    private def filterValue(value: Any): Option[Any] = value match {
      case str: String => if (str.nonEmpty) Some(str) else None
      case Some(o) => filterValue(o)
      case None => None
      case map: Map[String, Any] =>
        val filteredMap = removeEmptyValuesFromMap(map)
        if (filteredMap.nonEmpty) Some(filteredMap) else None
      case x => Some(x)
    }

    private def validResponse(orderNumber: String) = Map(
      "validationErrors" -> null,
      "response" -> Map(
        "type" -> "AuthResponse",
        "currencyCode" -> currencyAmount.currency,
        "lineItems" -> Seq(
          "description" -> "",
          "duration" -> null,
          "options" -> Seq.empty,
          "price" -> currencyAmount.amount,
          "quantity" -> "1",
          "recurrence" -> null,
          "startupFee" -> null,
          "productId" -> "",
          "tangible" -> "N",
          "name" -> "123",
          "type" -> "product"
        ),
        "transactionId" -> "9093733405923",
        "billingAddr" -> responseBillingAddressMap,
        "shippingAddr" -> responseShippingAddressMap,
        "merchantOrderId" -> deal.invoiceId.get,
        "orderNumber" -> orderNumber,
        "recurrentInstallmentId" -> null,
        "responseMsg" -> "Successfully authorized the provided credit card",
        "responseCode" -> "APPROVED",
        "total" -> currencyAmount.amount,
        "errors" -> null
      ),
      "exception" -> null
    )
  }
}
