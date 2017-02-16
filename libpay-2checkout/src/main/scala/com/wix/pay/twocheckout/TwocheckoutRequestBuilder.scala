package com.wix.pay.twocheckout

import java.util.Locale

import com.wix.pay.creditcard.CreditCard
import com.wix.pay.model.{CurrencyAmount, Customer, Deal}
import org.json4s.JsonDSL._
import org.json4s._

class TwocheckoutRequestBuilder(defaultEmail: String = "example@example.org",
                                defaultPhone: String = "000",
                                defaultUnavailableValue: String = "NA") {

  def saleRequestContent(merchant: TwocheckoutMerchant,
                         token: String,
                         creditCard: CreditCard,
                         currencyAmount: CurrencyAmount,
                         customer: Option[Customer],
                         deal: Option[Deal]) = {
    val billingAddress = creditCard.billingAddressDetailed
    val shippingAddress = deal.flatMap(_.shippingAddress)
    JObject(
      "sellerId" -> merchant.sellerId,
      "privateKey" -> merchant.privateKey,
      "token" -> token,
      "merchantOrderId" -> deal.flatMap(_.invoiceId).orNA,
      "currency" -> currencyAmount.currency,
      "total" -> currencyAmount.amount,
      "billingAddr" -> JObject(
        "name" -> creditCard.holderName.orNA,
        "addrLine1" -> billingAddress.flatMap(_.street).orNA,
        "addrLine2" -> defaultUnavailableValue,
        "city" -> billingAddress.flatMap(_.city).orNA,
        "country" -> constructCountry(billingAddress.flatMap(_.countryCode)),
        "state" -> billingAddress.flatMap(_.state).orNA,
        "zipCode" -> billingAddress.flatMap(_.postalCode).orNA,
        "phoneNumber" -> (customer.flatMap(_.phone).filterEmpty.getOrElse(defaultPhone): String),
        "email" -> (customer.flatMap(_.email).filterEmpty.getOrElse(defaultEmail): String)
      ),
      "shippingAddr" -> JObject(
        "name" -> constructName(shippingAddress.flatMap(_.firstName), shippingAddress.flatMap(_.lastName)),
        "addrLine1" -> shippingAddress.flatMap(_.street).orNA,
        "addrLine2" -> defaultUnavailableValue,
        "city" -> shippingAddress.flatMap(_.city).orNA,
        "country" -> constructCountry(shippingAddress.flatMap(_.countryCode)),
        "state" -> shippingAddress.flatMap(_.state).orNA,
        "zipCode" -> shippingAddress.flatMap(_.postalCode).orNA
      )
    )
  }

  private def constructName(firstName: Option[String], lastName: Option[String]): String = {
    val name = s"${firstName.getOrElse("")} ${lastName.getOrElse("")}".trim
    if (name.nonEmpty) {
      name
    } else {
      defaultUnavailableValue
    }
  }

  private def constructCountry(countryCode: Option[Locale]): String =
    countryCode.map(_.getISO3Country.toUpperCase).orNA

  private implicit class OptionStringExtensions(o: Option[String]) {
    def filterEmpty: Option[String] = o.filter(_.trim.nonEmpty)
    def orNA: String = o.filterEmpty.getOrElse(defaultUnavailableValue)
  }
}

