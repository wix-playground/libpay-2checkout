package com.wix.pay.twocheckout

import java.io.{BufferedReader, InputStreamReader}
import java.util.stream.Collectors

import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.http.{ByteArrayContent, GenericUrl, HttpRequestFactory}
import com.wix.pay.creditcard.{CreditCard, CreditCardOptionalFields, YearMonth}
import com.wix.pay.twocheckout.model.Environments
import com.wix.pay.twocheckout.tokenization.html.HtmlTokenizer
import org.json4s.DefaultFormats
import org.json4s.native.Serialization

/**
  * Can be used to test and/or record 2Checkout requests.
  */
object TwocheckoutFakeClient extends App {
  implicit val formats = DefaultFormats

  // Replace with your credentials
  val sellerId = "yourSellerId"
  val publishableKey = "yourPublishableKey"
  val privateKey = "yourPrivateKey"

  val creditCard = CreditCard("4000000000000002", YearMonth(2020, 12), Some(CreditCardOptionalFields.withFields(
    csc = Some("123"), holderName = Some("John Smith"))
    ))

  val tokenizer = new HtmlTokenizer(Environments.sandbox)

  val token = tokenizer.tokenize(sellerId, publishableKey, creditCard).get
  println("token = " + token)

  val requestFactory: HttpRequestFactory = new NetHttpTransport().createRequestFactory()

  val requestContent = Map(
    "sellerId" -> sellerId,
    "privateKey" -> privateKey,
    "token" -> token,
    "merchantOrderId" -> "test123",
    "currency" -> "USD",
    "total" -> "1.00",
    "billingAddr" -> Map(
      "name" -> "Testing Tester",
      "addrLine1" -> "xvxcvxcvxcvcx",
      "city" -> "Columbus",
      "state" -> "Ohio",
      "country" -> "USA",
      "zipCode" -> "43230",
      "email" -> "tester@2co.com",
      "phoneNumber" -> "555-555-5555"
    ),
    "shippingAddr" -> Map(
      "name" -> "Ship toMe",
      "addrLine1" -> "xvxcvxcvxcvcx2",
      "city" -> "Columbus",
      "state" -> "Ohio",
      "country" -> "USA",
      "zipCode" -> "43230"
    )
  )

  val response = requestFactory.buildPostRequest(
    new GenericUrl(s"https://sandbox.2checkout.com/checkout/api/1/$sellerId/rs/authService"),
    new ByteArrayContent(
      "application/json",
      Serialization.write(requestContent).getBytes("UTF-8")
    )
  ).execute()

  val result = new BufferedReader(new InputStreamReader(response.getContent)).lines().collect(Collectors.joining("\n"))
  println(result)
}
