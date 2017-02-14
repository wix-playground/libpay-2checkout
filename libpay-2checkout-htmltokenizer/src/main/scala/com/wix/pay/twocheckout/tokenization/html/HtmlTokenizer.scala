package com.wix.pay.twocheckout.tokenization.html

import java.net.URLEncoder

import com.gargoylesoftware.htmlunit.{AlertHandler, Page, WebClient}
import com.wix.pay.creditcard.CreditCard
import com.wix.pay.twocheckout.model.Environments
import com.wix.pay.twocheckout.model.html.{Error, TokenizeResponse}
import com.wix.pay.twocheckout.tokenization.TwocheckoutTokenizer
import org.json4s.DefaultFormats
import org.json4s.native.Serialization

import scala.util.Try

object JavascriptSdkUrls {
  val production = "https://www.2checkout.com/checkout/api/2co.min.js"
}

/**
  * [[TwocheckoutTokenizer]] that uses 2checkout's official JavaScript SDK inside a headless browser.
  */
class HtmlTokenizer(jsSdkUrl: String = JavascriptSdkUrls.production,
                    environment: String = Environments.production) extends TwocheckoutTokenizer {
  override def tokenize(sellerId: String, publishableKey: String, card: CreditCard): Try[String] = {
    Try {
      val webClient = new WebClient
      try {
        var alertMessage: Option[String] = None
        webClient.setAlertHandler(new AlertHandler {
          override def handleAlert(page: Page, s: String): Unit = {
            alertMessage = Option(s)
          }
        })

        val tokenizeHtml = HtmlTokenizer.createTokenizeHtml(
          jsSdkUrl = jsSdkUrl,
          environment = environment,
          sellerId = sellerId,
          publishableKey = publishableKey,
          card = card
        )

        webClient.getPage(s"data:text/html,${URLEncoder.encode(tokenizeHtml, "UTF-8")}")

        val responseJson = alertMessage.get
        val response = HtmlTokenizer.parseHtmlTokenizerResponseJson(responseJson)
        (response.error, response.value) match {
          case (Some(error), _) =>
            throw new RuntimeException(s"${error.errorCode}|${error.errorMsg}")
          case (None, Some(value)) =>
            value.response.token.token
          case _ => throw new IllegalStateException(s"Tokenize HTML returned unexpected response format: $responseJson")
        }

      } finally {
        webClient.close()
      }
    }
  }
}

private object HtmlTokenizer {
  private implicit val formats = DefaultFormats

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
  def createTokenizeHtml(jsSdkUrl: String, environment: String, sellerId: String, publishableKey: String, card: CreditCard): String = {
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