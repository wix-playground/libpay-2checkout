package com.wix.pay.twocheckout.testkit


import org.apache.commons.codec.binary.Base64
import org.json4s.{DefaultFormats, Formats}
import org.json4s.native.Serialization
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model._
import com.wix.e2e.http.api.StubWebServer
import com.wix.e2e.http.client.extractors.HttpMessageExtractors._
import com.wix.e2e.http.server.WebServerFactory.aStubWebServer
import com.wix.pay.creditcard.CreditCard
import com.wix.pay.model.{CurrencyAmount, Customer, Deal, ShippingAddress}
import com.wix.pay.twocheckout.tokenization.RSAPublicKey


class TwocheckoutDriver(port: Int) {
  private implicit val format: Formats = DefaultFormats
  private val server: StubWebServer = aStubWebServer.onPort(port).build

  def start(): Unit = server.start()
  def stop(): Unit = server.stop()
  def reset(): Unit = server.replaceWith()


  def aSaleRequest(sellerId: String,
                   privateKey: String,
                   token: String,
                   creditCard: CreditCard,
                   currencyAmount: CurrencyAmount,
                   customer: Option[Customer],
                   deal: Option[Deal]): SaleRequest = {
    SaleRequest(sellerId, privateKey, token, creditCard, currencyAmount, customer, deal)
  }

  def aPublicKeyRequest(): PublicKeyRequest = PublicKeyRequest()

  def aPretokenRequest(sellerId: String, publishableKey: String) = PretokenRequest(sellerId, publishableKey)

  def aTokenRequest(sellerId: String): TokenRequest = TokenRequest(sellerId)

  case class SaleRequest(sellerId: String,
                         privateKey: String,
                         token: String,
                         creditCard: CreditCard,
                         currencyAmount: CurrencyAmount,
                         customer: Option[Customer],
                         deal: Option[Deal]) {
    private val path = s"/checkout/api/1/$sellerId/rs/authService"

    def returns(orderNumber: String): Unit = {
      val response = removeEmptyValuesFromMap(validResponse(orderNumber))

      respondWith(StatusCodes.OK, toJson(response))
    }

    def getsRejectedWith(message: String): Unit = respondWith(StatusCodes.BadRequest, toJson(rejected(message)))
    def getsAnErrorWith(message: String): Unit = {
      respondWith(StatusCodes.InternalServerError, toJson(internalError(message)))
    }

    private def rejected(message: String) = errorResponse(message, httpStatus = 400, errorCode = 607)
    private def internalError(message: String) = errorResponse(message, httpStatus = 500, errorCode = 400)

    private def errorResponse(errorDescription: String, httpStatus: Int, errorCode: Int) = Map(
      "validationErrors" -> null,
      "response" -> null,
      "exception" -> Map(
        "errorMsg" -> errorDescription,
        "httpStatus" -> httpStatus.toString,
        "exception" -> false,
        "errorCode" -> errorCode.toString))

    private def respondWith(status: StatusCode, content: String): Unit = {
      server.appendAll {
        case HttpRequest(HttpMethods.POST, Path(`path`), _, entity, _) if isStubbedEntity(entity) =>
          HttpResponse(status = status, entity = content)
      }
    }

    private def isStubbedEntity(entity: HttpEntity): Boolean = {
      val actual = toMap(entity)
      val expected = removeEmptyValuesFromMap(expectedJsonBody)

      entity.contentType.mediaType == MediaTypes.`application/json` &&
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
      "shippingAddr" -> requestShippingAddressMap)

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
      "state" -> billingAddress.flatMap(_.state).getOrElse("NA"))
    private val responseBillingAddressMap = requestBillingAddressMap ++ Map("phoneExtension" -> null)

    private val shippingAddress = deal.flatMap(_.shippingAddress)
    private val requestShippingAddressMap = Map(
      "addrLine1" -> shippingAddress.flatMap(_.street).getOrElse("NA"),
      "addrLine2" -> "NA",
      "city" -> shippingAddress.flatMap(_.city).getOrElse("NA"),
      "zipCode" -> shippingAddress.flatMap(_.postalCode).getOrElse("NA"),
      "country" -> shippingAddress.flatMap(_.countryCode).map(_.getISO3Country.toUpperCase).getOrElse("NA"),
      "name" -> shippingAddressName(shippingAddress),
      "state" -> shippingAddress.flatMap(_.state).getOrElse("NA"))
    private val responseShippingAddressMap = requestShippingAddressMap ++ Map(
      "phoneNumber" -> null,
      "phoneExtension" -> null,
      "email" -> null)

    private def shippingAddressName(shippingAddress: Option[ShippingAddress]): String = {
      val firstName = shippingAddress.flatMap(_.firstName).getOrElse("")
      val lastName = shippingAddress.flatMap(_.lastName).getOrElse("")
      val name = s"$firstName $lastName".trim
      if (name.nonEmpty) name else "NA"
    }

    private def toJson(map: Map[_, Any]): String = Serialization.write(map)
    private def toMap(entity: HttpEntity): Map[String, Any] = {
      Serialization.read[Map[String, Any]](entity.extractAsString)
    }

    private def removeEmptyValuesFromMap(map: Map[_, Any]): Map[_, Any] = map.flatMap { case (key, value) =>
      val filteredValue = filterValue(value)
      filteredValue.map(key -> _)
    }

    private def filterValue(value: Any): Option[Any] = value match {
      case str: String if str.nonEmpty =>
        Some(str)

      case _: String =>
        None

      case Some(o) =>
        filterValue(o)

      case None =>
        None

      case map: Map[_, Any] =>
        val filteredMap = removeEmptyValuesFromMap(map)
        if (filteredMap.nonEmpty) Some(filteredMap) else None

      case x =>
        Some(x)
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
          "type" -> "product"),
        "transactionId" -> "9093733405923",
        "billingAddr" -> responseBillingAddressMap,
        "shippingAddr" -> responseShippingAddressMap,
        "merchantOrderId" -> deal.flatMap(_.invoiceId).getOrElse("NA"),
        "orderNumber" -> orderNumber,
        "recurrentInstallmentId" -> null,
        "responseMsg" -> "Successfully authorized the provided credit card",
        "responseCode" -> "APPROVED",
        "total" -> currencyAmount.amount,
        "errors" -> null),
      "exception" -> null)
  }


  case class PublicKeyRequest() {
    def returns(publicKey: RSAPublicKey): Unit = {
      respondWith(
        StatusCodes.Accepted,
        s"""
           first=1234;
           publicKey={'m':'${publicKey.base64Modulus}','e':'${publicKey.base64Exp}'};
           anotherField='asdf';
         """.stripMargin)
    }

    def failsWith(message: String): Unit = {
      respondWith(StatusCodes.InternalServerError, message)
    }

    def returnsNoKey(): Unit = {
      respondWith(StatusCodes.Accepted,
        s"""
           first=1234;
           anotherField='asdf';
         """.stripMargin)
    }

    private def respondWith(status: StatusCode, content: String): Unit = {
      server.appendAll {
        case HttpRequest(HttpMethods.GET, requestPath, _, _, _)
          if requestPath.path startsWith Path(s"/checkout/api/script/publickey/") =>
            HttpResponse(status = status, entity = content)
      }
    }
  }


  case class PretokenRequest(sellerId: String, publishableKey: String) {
    def returns(preToken: String): Unit = {
      respondWith(
        StatusCodes.Accepted,
        s"""{"response" : {
           |    "type":"PreTokenResponse",
           |    "preToken":"$preToken"
           |  },
           |  "exception" : null
           |}""".stripMargin)
    }

    def failsWith(message: String, status: Int): Unit = {
      respondWith(
        StatusCodes.Accepted,
        s"""{"exception":{
           |    "errorMsg":"$message",
           |    "httpStatus":"$status",
           |    "exception":false,
           |    "errorCode":"900"
           |  },
           |  "response": null
           |}""".stripMargin)
    }

    private def isStubbed(uri: Uri): Boolean = {
      val payload = uri.query().getOrElse("payload", "{}")
      val json = Serialization.read[Map[String, String]](payload)

      uri.path == Path(s"/checkout/api/1/$sellerId/rs/preTokenService") &&
        json("sellerId") == sellerId &&
        json("publicKey") == Base64.encodeBase64String(publishableKey.getBytes("utf-8"))
    }

    private def respondWith(status: StatusCode, content: String): Unit = {
      server.appendAll {
        case HttpRequest(HttpMethods.GET, uri, _, _, _) if isStubbed(uri) =>
          HttpResponse(status = status, entity = content)
      }
    }
  }

  case class TokenRequest(sellerId: String) {
    def returns(token: String): Unit = {
      respondWith(
        StatusCodes.Accepted,
        s"""{"response" : {
           |    "type":"TokenResponse",
           |    "token":{
           |      "dateCreated":1488854095833,
           |      "token":"$token"
           |    }
           |  },
           |  "exception" : null
           |}""".stripMargin)
    }

    def failsWith(message: String, status: Int): Unit = {
      respondWith(
        StatusCodes.Accepted,
        s"""{"exception":{
           |    "errorMsg":"$message",
           |    "httpStatus":"$status",
           |    "exception":false,
           |    "errorCode":"900"
           |  },
           |  "response": null
           |}""".stripMargin)
    }

    //TODO: add check for encrypted content
    private def isStubbed(uri: Uri): Boolean = {
      val payload = uri.query().getOrElse("payload", "{}")
      val json = Serialization.read[Map[String, String]](payload)

      uri.path == Path(s"/checkout/api/1/$sellerId/rs/tokenService") && json("sellerId") == sellerId
    }

    private def respondWith(status: StatusCode, content: String): Unit = {
      server.appendAll {
        case HttpRequest(HttpMethods.GET, uri, _, _, _) if isStubbed(uri) =>
          HttpResponse(status = status, entity = content)
      }
    }
  }
}
