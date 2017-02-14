package com.wix.pay.twocheckout.testkit

import com.wix.hoopoe.http.testkit.EmbeddedHttpProbe
import com.wix.pay.twocheckout.model.html.{Error, PaymentMethod, Token, TokenResponse, TokenizeResponse}
import org.json4s.DefaultFormats
import org.json4s.native.Serialization
import spray.http._

class TwocheckoutJavascriptSdkDriver(port: Int) {
  private val probe = new EmbeddedHttpProbe(port, EmbeddedHttpProbe.NotFoundHandler)

  def reset(): Unit = probe.reset()

  def start(): Unit = probe.doStart()

  def stop(): Unit = probe.doStop()

  def aJavascriptSdkRequest(): JavascriptSdkRequest = {
    JavascriptSdkRequest()
  }

  case class JavascriptSdkRequest() {
    def successfullyTokenizes(sellerId: String,
                              publishableKey: String,
                              ccNo: String,
                              cvv: String,
                              expMonth: Int,
                              expYear: Int,
                              environment: String,
                              token: String): Unit = {
      val javascript = TwocheckoutJavascriptSdkDriver.buildSucceedingJavascriptSdk(
        sellerId = sellerId,
        publishableKey = publishableKey,
        ccNo = ccNo,
        cvv = cvv,
        expMonth = expMonth,
        expYear = expYear,
        environment = environment,
        response = TokenizeResponse(
          response = TokenResponse(
            `type` = "TokenResponse",
            token = Token(
              dateCreated = System.currentTimeMillis,
              token = token
            ),
            paymentMethod = PaymentMethod(
              cardNum = s"XXXX-XXXX-XXXX-${ccNo.substring(ccNo.length - 4)}",
              expMonth = expMonth.toString,
              expYear = expYear.toString,
              cardType = "VS"
            )
          )
        )
      )
      respondWith(javascript)
    }

    def failsTokenizing(sellerId: String,
                        publishableKey: String,
                        ccNo: String,
                        cvv: String,
                        expMonth: Int,
                        expYear: Int,
                        environment: String,
                        error: Error): Unit = {
      val javascript = TwocheckoutJavascriptSdkDriver.buildFailingJavascriptSdk(
        sellerId = sellerId,
        publishableKey = publishableKey,
        ccNo = ccNo,
        cvv = cvv,
        expMonth = expMonth,
        expYear = expYear,
        environment = environment,
        error = error
      )
      respondWith(javascript)
    }

    private def respondWith(javascript: String): Unit = {
      probe.handlers += {
        case HttpRequest(HttpMethods.GET, Uri.Path("/"), _, _, _) =>
          HttpResponse(StatusCodes.OK, HttpEntity(ContentType(MediaTypes.`application/javascript`), javascript))
      }
    }
  }
}

private object TwocheckoutJavascriptSdkDriver {
  private implicit val formats = DefaultFormats

  def buildSucceedingJavascriptSdk(sellerId: String,
                                   publishableKey: String,
                                   ccNo: String,
                                   cvv: String,
                                   expMonth: Int,
                                   expYear: Int,
                                   environment: String,
                                   response: TokenizeResponse): String = {
    buildJavascriptSdk(
      sellerId = sellerId,
      publishableKey = publishableKey,
      ccNo = ccNo,
      cvv = cvv,
      expMonth = expMonth,
      expYear = expYear,
      environment = environment,
      requestTokenJs = s"onSuccess(${Serialization.write(response)});"
    )
  }

  def buildFailingJavascriptSdk(sellerId: String,
                                publishableKey: String,
                                ccNo: String,
                                cvv: String,
                                expMonth: Int,
                                expYear: Int,
                                environment: String,
                                error: Error): String = {
    buildJavascriptSdk(
      sellerId = sellerId,
      publishableKey = publishableKey,
      ccNo = ccNo,
      cvv = cvv,
      expMonth = expMonth,
      expYear = expYear,
      environment = environment,
      requestTokenJs = s"onError(${Serialization.write(error)});"
    )
  }

  private def buildJavascriptSdk(sellerId: String,
                                 publishableKey: String,
                                 ccNo: String,
                                 cvv: String,
                                 expMonth: Int,
                                 expYear: Int,
                                 environment: String,
                                 requestTokenJs: String): String = {
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
       |    if (params.ccNo !== '$ccNo') {
       |      throw new Error('Unexpected ccNo');
       |    }
       |    if (params.cvv !== '$cvv') {
       |      throw new Error('Unexpected cvv');
       |    }
       |    if (params.expMonth !== $expMonth) {
       |      throw new Error('Unexpected expMonth');
       |    }
       |    if (params.expYear !== $expYear) {
       |      throw new Error('Unexpected expYear');
       |    }
       |    $requestTokenJs
       |  }
       |};
    """.stripMargin
  }
}