package com.wix.pay.twocheckout

import java.util.Locale

import com.wix.pay.creditcard.CreditCard
import com.wix.pay.model.{CurrencyAmount, Customer, Deal}
import org.json4s.JsonDSL._
import org.json4s.{JObject, JString, _}
import org.specs2.mutable.SpecWithJUnit
import org.specs2.specification.Scope

class TwocheckoutRequestBuilderTest extends SpecWithJUnit with TwocheckoutTestSupport {
  "TwocheckoutRequestBuilder" should {
    "fill in all parameters when they are available" in new Ctx {
      saleRequest() mustEqual JObject(
        "sellerId" -> sellerId,
        "privateKey" -> privateKey,
        "token" -> token,
        "merchantOrderId" -> deal.invoiceId.get,
        "currency" -> currencyAmount.currency,
        "total" -> currencyAmount.amount,
        "billingAddr" -> JObject(
          "name" -> creditCard.holderName.get,
          "addrLine1" -> billingAddress.street.get,
          "addrLine2" -> defaultNA,
          "city" -> billingAddress.city.get,
          "country" -> billingAddress.countryCode.get.getISO3Country,
          "state" -> billingAddress.state.get,
          "zipCode" -> billingAddress.postalCode.get,
          "phoneNumber" -> customer.phone.get,
          "email" -> customer.email.get
        ),
        "shippingAddr" -> JObject(
          "name" -> s"${shippingAddress.firstName.get} ${shippingAddress.lastName.get}",
          "addrLine1" -> shippingAddress.street.get,
          "addrLine2" -> defaultNA,
          "city" -> shippingAddress.city.get,
          "country" -> shippingAddress.countryCode.get.getISO3Country,
          "state" -> shippingAddress.state.get,
          "zipCode" -> shippingAddress.postalCode.get
        )
      )
    }

    "use the default unavailable value for billing name when credit card holderName is missing" in new Ctx {
      saleRequest(withCreditCard = creditCard.withoutHolderName).billingName must beNA
    }

    "use the default unavailable value for billing name when credit card holderName is empty" in new Ctx {
      saleRequest(withCreditCard = creditCard.withHolderName("")).billingName must beNA
    }

    "use the default unavailable value for billing street when credit card billing address is missing" in new Ctx {
      saleRequest(withCreditCard = creditCard.withoutBillingAddress).billingStreet must beNA
    }

    "use the default unavailable value for billing street when credit card billing address street is missing" in new Ctx {
      saleRequest(withCreditCard = creditCard.withBillingAddress(_.withoutStreet)).billingStreet must beNA
    }

    "use the default unavailable value for billing street when credit card billing address street is empty" in new Ctx {
      saleRequest(withCreditCard = creditCard.withBillingAddress(_.withStreet(""))).billingStreet must beNA
    }

    "use the default unavailable value for billing street2 when credit card billing address is missing" in new Ctx {
      saleRequest(withCreditCard = creditCard.withoutBillingAddress).billingStreet2 must beNA
    }

    "use the default unavailable value for billing street when credit card billing address is present" in new Ctx {
      saleRequest(withCreditCard = creditCard).billingStreet2 must beNA
    }

    "use the default unavailable value for billing city when credit card billing address is missing" in new Ctx {
      saleRequest(withCreditCard = creditCard.withoutBillingAddress).billingCity must beNA
    }

    "use the default unavailable value for billing city when credit card billing address city is missing" in new Ctx {
      saleRequest(withCreditCard = creditCard.withBillingAddress(_.withoutCity)).billingCity must beNA
    }

    "use the default unavailable value for billing city when credit card billing address city is empty" in new Ctx {
      saleRequest(withCreditCard = creditCard.withBillingAddress(_.withCity(""))).billingCity must beNA
    }

    "use the default unavailable value for billing country when credit card billing address is missing" in new Ctx {
      saleRequest(withCreditCard = creditCard.withoutBillingAddress).billingCountry must beNA
    }

    "use the default unavailable value for billing country when credit card billing address country is missing" in new Ctx {
      saleRequest(withCreditCard = creditCard.withBillingAddress(_.withoutCountryCode)).billingCountry must beNA
    }

    "use the default unavailable value for billing country when credit card billing address country is empty" in new Ctx {
      saleRequest(withCreditCard = creditCard.withBillingAddress(_.withCountryCode(new Locale("")))).billingCountry must beNA
    }

    "use the default unavailable value for billing state when credit card billing address is missing" in new Ctx {
      saleRequest(withCreditCard = creditCard.withoutBillingAddress).billingState must beNA
    }

    "use the default unavailable value for billing state when credit card billing address state is missing" in new Ctx {
      saleRequest(withCreditCard = creditCard.withBillingAddress(_.withoutState)).billingState must beNA
    }

    "use the default unavailable value for billing state when credit card billing address state is empty" in new Ctx {
      saleRequest(withCreditCard = creditCard.withBillingAddress(_.withState(""))).billingState must beNA
    }

    "use the default unavailable value for billing postal code when credit card billing address is missing" in new Ctx {
      saleRequest(withCreditCard = creditCard.withoutBillingAddress).billingZipCode must beNA
    }

    "use the default unavailable value for billing postal code when credit card billing address postal code is missing" in new Ctx {
      saleRequest(withCreditCard = creditCard.withBillingAddress(_.withoutPostalCode)).billingZipCode must beNA
    }

    "use the default unavailable value for billing postal code when credit card billing address postal code is empty" in new Ctx {
      saleRequest(withCreditCard = creditCard.withBillingAddress(_.withPostalCode(""))).billingZipCode must beNA
    }

    "use the default phone number value for billing phone number when customer is missing" in new Ctx {
      saleRequest(withCustomer = None).billingPhoneNumber must beDefaultPhone
    }

    "use the default phone number value for billing phone number when customer phone number is missing" in new Ctx {
      saleRequest(withCustomer = Some(customer.withoutPhone)).billingPhoneNumber must beDefaultPhone
    }

    "use the default phone number value for billing phone number when customer phone number is empty" in new Ctx {
      saleRequest(withCustomer = Some(customer.withPhone(""))).billingPhoneNumber must beDefaultPhone
    }

    "use the default email value for billing email when customer is missing" in new Ctx {
      saleRequest(withCustomer = None).billingEmail must beDefaultEmail
    }

    "use the default email value for billing phone number when customer email is missing" in new Ctx {
      saleRequest(withCustomer = Some(customer.withoutEmail)).billingEmail must beDefaultEmail
    }

    "use the default email value for billing phone number when customer email is empty" in new Ctx {
      saleRequest(withCustomer = Some(customer.withEmail(""))).billingEmail must beDefaultEmail
    }

    "use the default unavailable value for invoiceId when deal is missing" in new Ctx {
      saleRequest(withDeal = None).merchantOrderId must beNA
    }

    "use the default unavailable value for invoiceId when deal invoiceId is missing" in new Ctx {
      saleRequest(withDeal = Some(deal.withoutInvoiceId)).merchantOrderId must beNA
    }

    "use the default unavailable value for invoiceId when deal invoiceId is empty" in new Ctx {
      saleRequest(withDeal = Some(deal.withInvoiceId(""))).merchantOrderId must beNA
    }

    "use the default unavailable value for shipping name when deal is missing" in new Ctx {
      saleRequest(withDeal = None).shippingName must beNA
    }

    "use the default unavailable value for shipping name when deal is missing" in new Ctx {
      saleRequest(withDeal = None).shippingName must beNA
    }

    "use the default unavailable value for shipping name when deal shipping first name & last name are absent" in new Ctx {
      saleRequest(withDeal = Some(deal.withShippingAddress(_.withoutFirstName.withoutLastName))).shippingName must beNA
    }

    "use the default unavailable value for shipping name when deal shipping first name & last name are empty" in new Ctx {
      saleRequest(withDeal = Some(deal.withShippingAddress(_.withFirstName("").withLastName("")))).shippingName must beNA
    }

    "use the default unavailable value for shipping name when deal shipping first name & last name are all whitespace" in new Ctx {
      saleRequest(withDeal = Some(deal.withShippingAddress(_.withFirstName(" ").withLastName("  ")))).shippingName must beNA
    }

    "use the default unavailable value for shipping street when deal shipping address is missing" in new Ctx {
      saleRequest(withDeal = Some(deal.withoutShippingAddress)).shippingStreet must beNA
    }

    "use the default unavailable value for shipping street when deal shipping address street is missing" in new Ctx {
      saleRequest(withDeal = Some(deal.withShippingAddress(_.withoutStreet))).shippingStreet must beNA
    }

    "use the default unavailable value for shipping street when deal shipping address is missing" in new Ctx {
      saleRequest(withDeal = Some(deal.withoutShippingAddress)).shippingStreet2 must beNA
    }

    "use the default unavailable value for shipping street when deal shipping address is present" in new Ctx {
      saleRequest(withDeal = Some(deal)).shippingStreet2 must beNA
    }

    "use the default unavailable value for shipping street when deal shipping address street is empty" in new Ctx {
      saleRequest(withDeal = Some(deal.withShippingAddress(_.withStreet("")))).shippingStreet must beNA
    }

    "use the default unavailable value for shipping city when deal shipping address is missing" in new Ctx {
      saleRequest(withDeal = Some(deal.withoutShippingAddress)).shippingCity must beNA
    }

    "use the default unavailable value for shipping city when deal shipping address city is missing" in new Ctx {
      saleRequest(withDeal = Some(deal.withShippingAddress(_.withoutCity))).shippingCity must beNA
    }

    "use the default unavailable value for shipping city when deal shipping address city is empty" in new Ctx {
      saleRequest(withDeal = Some(deal.withShippingAddress(_.withCity("")))).shippingCity must beNA
    }

    "use the default unavailable value for shipping country when deal shipping address is missing" in new Ctx {
      saleRequest(withDeal = Some(deal.withoutShippingAddress)).shippingCountry must beNA
    }

    "use the default unavailable value for shipping country when deal shipping address country is missing" in new Ctx {
      saleRequest(withDeal = Some(deal.withShippingAddress(_.withoutCountryCode))).shippingCountry must beNA
    }

    "use the default unavailable value for shipping country when deal shipping address country is empty" in new Ctx {
      saleRequest(withDeal = Some(deal.withShippingAddress(_.withCountryCode(new Locale(""))))).shippingCountry must beNA
    }

    "use the default unavailable value for shipping state when deal shipping address is missing" in new Ctx {
      saleRequest(withDeal = Some(deal.withoutShippingAddress)).shippingState must beNA
    }

    "use the default unavailable value for shipping state when deal shipping address state is missing" in new Ctx {
      saleRequest(withDeal = Some(deal.withShippingAddress(_.withoutState))).shippingState must beNA
    }

    "use the default unavailable value for shipping state when deal shipping address state is empty" in new Ctx {
      saleRequest(withDeal = Some(deal.withShippingAddress(_.withState("")))).shippingState must beNA
    }

    "use the default unavailable value for shipping postal code when deal shipping address is missing" in new Ctx {
      saleRequest(withDeal = Some(deal.withoutShippingAddress)).shippingZipCode must beNA
    }

    "use the default unavailable value for shipping postal code when deal shipping address postal code is missing" in new Ctx {
      saleRequest(withDeal = Some(deal.withShippingAddress(_.withoutPostalCode))).shippingZipCode must beNA
    }

    "use the default unavailable value for shipping postal code when deal shipping address postal code is empty" in new Ctx {
      saleRequest(withDeal = Some(deal.withShippingAddress(_.withPostalCode("")))).shippingZipCode must beNA
    }
  }

  trait Ctx extends Scope {
    val defaultEmail = "defaultEmail"
    val defaultPhone = "defaultPhone"
    val defaultNA = "defaultNA"
    val builder = new TwocheckoutRequestBuilder(defaultEmail, defaultPhone, defaultNA)

    def saleRequest(withCreditCard: CreditCard = creditCard,
                    withCurrencyAmount: CurrencyAmount = currencyAmount,
                    withCustomer: Option[Customer] = Some(customer),
                    withDeal: Option[Deal] = Some(deal)) =
      builder.saleRequestContent(someMerchant, token, withCreditCard, withCurrencyAmount, withCustomer, withDeal)

    def beDefaultEmail = beEqualTo(JString(defaultEmail))
    def beDefaultPhone = beEqualTo(JString(defaultPhone))
    def beNA = beEqualTo(JString(defaultNA))

    implicit class JObjectTestExtensions(o: JObject) {
      def merchantOrderId = o \\ "merchantOrderId"

      def billingAddr = o \\ "billingAddr"
      def billingName = billingAddr \\ "name"
      def billingStreet = billingAddr \\ "addrLine1"
      def billingStreet2 = billingAddr \\ "addrLine2"
      def billingCity = billingAddr \\ "city"
      def billingCountry = billingAddr \\ "country"
      def billingState = billingAddr \\ "state"
      def billingZipCode = billingAddr \\ "zipCode"
      def billingPhoneNumber = billingAddr \\ "phoneNumber"
      def billingEmail = billingAddr \\ "email"

      def shippingAddr = o \\ "shippingAddr"
      def shippingName = shippingAddr \\ "name"
      def shippingStreet = shippingAddr \\ "addrLine1"
      def shippingStreet2 = shippingAddr \\ "addrLine2"
      def shippingCity = shippingAddr \\ "city"
      def shippingCountry = shippingAddr \\ "country"
      def shippingState = shippingAddr \\ "state"
      def shippingZipCode = shippingAddr \\ "zipCode"
    }
  }
}
