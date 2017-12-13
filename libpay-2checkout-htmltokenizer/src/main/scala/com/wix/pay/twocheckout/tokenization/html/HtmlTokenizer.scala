package com.wix.pay.twocheckout.tokenization.html


import scala.util.Try
import java.net.URLEncoder
import org.json4s.{DefaultFormats, Formats}
import org.json4s.native.Serialization
import com.gargoylesoftware.htmlunit.{AlertHandler, Page, WebClient}
import com.wix.pay.creditcard.CreditCard
import com.wix.pay.twocheckout.model.TwocheckoutSettings
import com.wix.pay.twocheckout.model.html.{Error, TokenizeResponse}
import com.wix.pay.twocheckout.tokenization.TwocheckoutTokenizer


/** `TwocheckoutTokenizer` that uses 2checkout's official JavaScript SDK inside a headless browser.
  */
class HtmlTokenizer(settings: TwocheckoutSettings) extends TwocheckoutTokenizer {
  override def tokenize(sellerId: String,
                        publishableKey: String,
                        card: CreditCard,
                        sandboxMode: Boolean): Try[String] = {
    Try {
      val webClient = new WebClient
      webClient.getOptions.setCssEnabled(false)
      webClient.getJavaScriptEngine.getContextFactory.enterContext().setOptimizationLevel(9)

      try {
        var alertMessage: Option[String] = None
        webClient.setAlertHandler(new AlertHandler {
          override def handleAlert(page: Page, s: String): Unit = {
            alertMessage = Option(s)
          }
        })

        val tokenizeHtml = HtmlTokenizer.createTokenizeHtml(
          jsSdkUrl = settings.jsSdkUrl(sandboxMode),
          environment = settings.environment(sandboxMode),
          sellerId = sellerId,
          publishableKey = publishableKey,
          card = card)

        webClient.getPage(s"data:text/html,${URLEncoder.encode(tokenizeHtml, "UTF-8")}")

        val responseJson = alertMessage.get
        val response = HtmlTokenizer.parseHtmlTokenizerResponseJson(responseJson)

        (response.error, response.value) match {
          case (Some(error), _) =>
            throw new RuntimeException(s"${error.errorCode}|${error.errorMsg}")

          case (None, Some(value)) =>
            value.response.token.token

          case _ =>
            throw new IllegalStateException(s"Tokenize HTML returned unexpected response format: $responseJson")
        }
      } finally {
        webClient.close()
      }
    }
  }
}

private object HtmlTokenizer {
  private implicit val formats: Formats = DefaultFormats

  /**
    * @param value   Success value (None on error).
    * @param error   Error value (None on success).
    */
  case class HtmlTokenizerResponse(value: Option[TokenizeResponse], error: Option[Error])

  def parseHtmlTokenizerResponseJson(responseJson: String): HtmlTokenizerResponse = {
    Serialization.read[HtmlTokenizerResponse](responseJson)
  }

  /**
    * Creates an HTML that tokenizes a card on behalf of a seller.
    *
    * Communication with 2checkout is done using their official JavaScript SDK. The response, a JSON serialized
    * [[HtmlTokenizerResponse]], is output using window.alert.
    */
  def createTokenizeHtml(jsSdkUrl: String,
                         environment: String,
                         sellerId: String,
                         publishableKey: String,
                         card: CreditCard): String = {
    s"""
       |<!DOCTYPE html>
       |<html>
       |  <head>
       |    <title>2checkout tokenizer</title>
       |    <script src="$jsSdkUrl"></script>
       |  </head>
       |  <body>
       |    <script>
       |      function onSuccess(data) {
       |        alert(JSON.stringify({
       |          value: data
       |        }));
       |      }
       |
       |      function onError(data) {
       |        alert(JSON.stringify({
       |          error: data
       |        }));
       |      }
       |
       |      function onLoad() {
       |        TCO.requestToken(onSuccess, onError, {
       |          sellerId: '$sellerId',
       |          publishableKey: '$publishableKey',
       |          ccNo: '${card.number}',
       |          cvv: '${card.csc.get}',
       |          expMonth: ${card.expiration.month},
       |          expYear: ${card.expiration.year}
       |        });
       |      }
       |
       |      TCO.loadPubKey('$environment', onLoad);
       |    </script>
       |  </body>
       |</html>
    """.stripMargin
  }
}
