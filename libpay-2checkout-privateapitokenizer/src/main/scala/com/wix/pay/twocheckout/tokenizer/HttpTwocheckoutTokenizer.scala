package com.wix.pay.twocheckout.tokenizer

import java.net.URLEncoder

import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.http.{GenericUrl, HttpRequestFactory}
import com.wix.pay.PaymentErrorException
import com.wix.pay.creditcard.CreditCard
import com.wix.pay.twocheckout.model.TwocheckoutSettings
import com.wix.pay.twocheckout.tokenization.{RSAPublicKey, RSAUtils, TwocheckoutTokenizer}
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.util.Try

class HttpTwocheckoutTokenizer(settings: TwocheckoutSettings) extends TwocheckoutTokenizer {
  private implicit val formats = DefaultFormats

  private val requestBuilder = new TokenizerRequestBuilder
  private val requestFactory: HttpRequestFactory = new NetHttpTransport().createRequestFactory()

  override def tokenize(sellerId: String, publishableKey: String, card: CreditCard, sandboxMode: Boolean): Try[String] = {
    Try {
      val endpoint = settings.endpointUrl(sandboxMode)
      val key = publicKey(endpoint)
      val pretoken = preTokenFor(endpoint, sellerId, publishableKey)
      tokenFor(endpoint, sellerId, publishableKey, pretoken, key, card)
    }
  }

  private def tokenFor(endpointUrl: String, sellerId: String, publishableKey: String, preToken: String,
                       publicKey: RSAPublicKey, card: CreditCard): String = {
    val rawRequest = requestBuilder.innerTokenRequest(card, publishableKey, preToken)
    val encrypted = RSAUtils.rsaEncrypt(publicKey, rawRequest)
    val payload = URLEncoder.encode(requestBuilder.tokenRequest(sellerId, encrypted), "utf-8")

    val requestUrl = s"$endpointUrl/checkout/api/1/$sellerId/rs/tokenService?payload=$payload"
    withJsonResponseFor[String](requestUrl) { response =>
      response("response")
          .asInstanceOf[Map[String, Any]]("token")
          .asInstanceOf[Map[String, String]]("token")
    }
  }

  private def preTokenFor(endpointUrl: String, sellerId: String, publishableKey: String): String = {
    val payload = URLEncoder.encode(requestBuilder.pretokenRequest(sellerId, publishableKey), "utf-8")
    val requestUrl = s"$endpointUrl/checkout/api/1/$sellerId/rs/preTokenService?payload=$payload"
    withJsonResponseFor(requestUrl) { response =>
      response("response").asInstanceOf[Map[String, String]]("preToken")
    }
  }

  private def publicKey(endpointUrl: String): RSAPublicKey = {
    val requestUrl = s"$endpointUrl/checkout/api/script/publickey/${System.currentTimeMillis}"
    withResponseFor(requestUrl) { response =>
      val regex = "publicKey\\=(\\{.*\\});".r
      val maybePublicKey = for (json <- regex findFirstMatchIn response) yield json.group(1).replace("'", "\"")
      val publicKey = maybePublicKey.getOrElse(throw PaymentErrorException("Can't load 2checkout publicKey"))

      val parsed = parse(publicKey).extract[Map[String, String]]
      RSAPublicKey(parsed("m"), parsed("e"))
    }
  }

  private def withJsonResponseFor[T](url: String)(parser: Map[String, Any] => T): T = {
    withResponseFor(url) { response =>
      val map = parse(response).extract[Map[String, Any]]
      if (map.get("exception").exists(_ != null)) {
        val error = Extraction.decompose(map("exception"))
        throw PaymentErrorException(compact(render(error)))
      }
      parser(map)
    }
  }

  private def withResponseFor[T](url: String)(parser: String => T): T = {
    val response = requestFactory.buildGetRequest(new GenericUrl(url)).execute()
    val body = response.parseAsString
    parser(body)
  }
}
