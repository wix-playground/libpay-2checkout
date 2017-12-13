package com.wix.pay.twocheckout.testkit


import org.json4s.{DefaultFormats, Formats}
import org.json4s.native.Serialization
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model._
import com.wix.e2e.http.api.StubWebServer
import com.wix.e2e.http.server.WebServerFactory.aStubWebServer
import com.wix.pay.creditcard.CreditCard
import com.wix.pay.twocheckout.model.html.{Error, PaymentMethod, Token, TokenResponse, TokenizeResponse}


class TwocheckoutJavascriptSdkDriver(port: Int) {
  private implicit val format: Formats = DefaultFormats
  private val server: StubWebServer = aStubWebServer.onPort(port).build

  def start(): Unit = server.start()
  def stop(): Unit = server.stop()
  def reset(): Unit = server.replaceWith()


  def aJavascriptSdkRequest(sellerId: String,
                            publishableKey: String,
                            environment: String,
                            creditCard: CreditCard): JavascriptSdkRequest = {
    JavascriptSdkRequest(sellerId, publishableKey, environment, creditCard)
  }


  case class JavascriptSdkRequest(sellerId: String,
                                  publishableKey: String,
                                  environment: String,
                                  creditCard: CreditCard) {
    def successfullyTokenizes(token: String): Unit = {
      val javascript = buildSucceedingJavascriptSdk(response = TokenizeResponse(
        response = TokenResponse(
          `type` = "TokenResponse",
          token = Token(
            dateCreated = System.currentTimeMillis,
            token = token),
          paymentMethod = PaymentMethod(
            cardNum = s"XXXX-XXXX-XXXX-${creditCard.number.takeRight(4)}",
            expMonth = creditCard.expiration.month.toString,
            expYear = creditCard.expiration.year.toString,
            cardType = "VS"))))

      respondWith(javascript)
    }

    def failsTokenizing(error: Error): Unit = {
      val javascript = buildFailingJavascriptSdk(error = error)
      respondWith(javascript)
    }

    private def respondWith(javascript: String): Unit = {
      server.appendAll {
        case HttpRequest(HttpMethods.GET, Path("/"), _, _, _) =>
          HttpResponse(
            status = StatusCodes.OK,
            entity = HttpEntity(ContentType(MediaTypes.`application/javascript`, HttpCharsets.`UTF-8`), javascript))
      }
    }

    private def buildSucceedingJavascriptSdk(response: TokenizeResponse): String = {
      buildJavascriptSdk(requestTokenJs = s"onSuccess(${Serialization.write(response)});")
    }

    private def buildFailingJavascriptSdk(error: Error): String = {
      buildJavascriptSdk(requestTokenJs = s"onError(${Serialization.write(error)});")
    }

    private def buildJavascriptSdk(requestTokenJs: String): String = {
      s"""
         |var TCO = {
         |  loadPubKeyCalled: false,
         |  loadPubKeyReturned: false,
         |  loadPubKey: function(environment, onLoad) {
         |    if (TCO.loadPubKeyCalled) {
         |      throw new Error('loadPubKey already called');
         |    }
         |    TCO.loadPubKeyCalled = true;
         |    if (environment !== "$environment") {
         |      throw new Error('Attempt to load unexpected environment');
         |    }
         |    TCO.loadPubKeyReturned = true;
         |    onLoad();
         |  },
         |  requestToken: function(onSuccess, onError, params) {
         |    if (!TCO.loadPubKeyCalled) {
         |      throw new Error('requestToken called before loadPubKey');
         |    }
         |    if (!TCO.loadPubKeyReturned) {
         |      throw new Error('requestToken called before loadPubKey returned');
         |    }
         |    if (params.sellerId !== "$sellerId") {
         |      throw new Error('Unexpected sellerId');
         |    }
         |    if (params.publishableKey !== "$publishableKey") {
         |      throw new Error('Unexpected publishableKey');
         |    }
         |    if (params.ccNo !== '${creditCard.number}') {
         |      throw new Error('Unexpected ccNo');
         |    }
         |    if (params.cvv !== '${creditCard.csc.get}') {
         |      throw new Error('Unexpected cvv');
         |    }
         |    if (params.expMonth !== ${creditCard.expiration.month}) {
         |      throw new Error('Unexpected expMonth');
         |    }
         |    if (params.expYear !== ${creditCard.expiration.year}) {
         |      throw new Error('Unexpected expYear');
         |    }
         |    $requestTokenJs
         |  }
         |};
    """.stripMargin
    }
  }
}