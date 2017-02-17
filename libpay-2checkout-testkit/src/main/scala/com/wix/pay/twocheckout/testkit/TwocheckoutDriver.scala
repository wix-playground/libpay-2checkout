package com.wix.pay.twocheckout.testkit

import com.wix.hoopoe.http.testkit.EmbeddedHttpProbe
import com.wix.pay.creditcard.CreditCard
import com.wix.pay.model.{CurrencyAmount, Customer, Deal, ShippingAddress}
import org.json4s.DefaultFormats
import org.json4s.native.Serialization
import spray.http.Uri.Path
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
                   customer: Option[Customer],
                   deal: Option[Deal]) = {
    SaleRequest(sellerId, privateKey, token, creditCard, currencyAmount, customer, deal)
  }

  case class SaleRequest(sellerId: String,
                         privateKey: String,
                         token: String,
                         creditCard: CreditCard,
                         currencyAmount: CurrencyAmount,
                         customer: Option[Customer],
                         deal: Option[Deal]) {

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
          if requestPath.path == Path(s"/checkout/api/1/$sellerId/rs/authService") &&
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
      "merchantOrderId" -> deal.flatMap(_.invoiceId).getOrElse("NA"),
      "currency" -> currencyAmount.currency,
      "total" -> currencyAmount.amount,
      "billingAddr" -> requestBillingAddressMap,
      "shippingAddr" -> requestShippingAddressMap
    )

    private val billingAddress = creditCard.billingAddressDetailed
    private val requestBillingAddressMap = Map(
      "addrLine1" -> billingAddress.flatMap(_.street).getOrElse("NA"),
      "addrLine2" -> "NA",
      "city" -> billingAddress.flatMap(_.city).getOrElse("NA"),
      "zipCode" -> billingAddress.flatMap(_.postalCode).getOrElse("NA"),
      "phoneNumber" -> customer.flatMap(_.phone).getOrElse("000"),
      "email" -> customer.flatMap(_.email).getOrElse("example@example.org"),
      "country" -> billingAddress.flatMap(_.countryCode).map(_.getISO3Country.toUpperCase).getOrElse("NA"),
      "name" -> creditCard.holderName.getOrElse("NA"),
      "state" -> billingAddress.flatMap(_.state).getOrElse("NA")
    )
    private val responseBillingAddressMap = requestBillingAddressMap ++ Map(
      "phoneExtension" -> null
    )

    private val shippingAddress = deal.flatMap(_.shippingAddress)
    private val requestShippingAddressMap = Map(
      "addrLine1" -> shippingAddress.flatMap(_.street).getOrElse("NA"),
      "addrLine2" -> "NA",
      "city" -> shippingAddress.flatMap(_.city).getOrElse("NA"),
      "zipCode" -> shippingAddress.flatMap(_.postalCode).getOrElse("NA"),
      "country" -> shippingAddress.flatMap(_.countryCode).map(_.getISO3Country.toUpperCase).getOrElse("NA"),
      "name" -> shippingAddressName(shippingAddress),
      "state" -> shippingAddress.flatMap(_.state).getOrElse("NA")
    )
    private val responseShippingAddressMap = requestShippingAddressMap ++ Map(
      "phoneNumber" -> null,
      "phoneExtension" -> null,
      "email" -> null
    )

    private def shippingAddressName(shippingAddress: Option[ShippingAddress]): String = {
      val name = s"${shippingAddress.flatMap(_.firstName).getOrElse("")} ${shippingAddress.flatMap(_.lastName).getOrElse("")}".trim
      if (name.nonEmpty) name else "NA"
    }

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
        "merchantOrderId" -> deal.flatMap(_.invoiceId).getOrElse("NA"),
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
