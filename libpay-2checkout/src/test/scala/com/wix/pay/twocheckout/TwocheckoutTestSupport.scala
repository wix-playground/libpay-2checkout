package com.wix.pay.twocheckout

import java.util.Locale

import com.wix.pay.creditcard._
import com.wix.pay.model._
import com.wix.pay.{PaymentErrorException, PaymentRejectedException}
import org.json4s.ParserUtil.ParseException
import org.specs2.matcher.Matcher
import org.specs2.matcher.MustThrownMatchers._

import scala.reflect.ClassTag
import scala.util.Try

trait TwocheckoutTestSupport {
  val sellerId = "someSellerId"
  val publishableKey = "somePublishableKey"
  val privateKey = "somePrivateKey"
  val token = "someToken"
  val someMerchant = TwocheckoutMerchant(sellerId, publishableKey, privateKey)
  val someMerchantStr = JsonTwocheckoutMerchantParser.stringify(someMerchant)

  val billingAddress = AddressDetailed(
    street = Some("billingStreet"),
    city = Some("billingCity"),
    state = Some("billingState"),
    postalCode = Some("billingPostalCode"),
    countryCode = Some(Locale.CANADA)
  )
  
  val shippingAddress = ShippingAddress(
    firstName = Some("shippingFirstName"),
    lastName = Some("shippingLastName"),
    address = Some(AddressDetailed(
      street = Some("shippingStreet"),
      city = Some("shippingCity"),
      state = Some("shippingState"),
      postalCode = Some("shippingPostalCode"),
      countryCode = Some(Locale.FRANCE)
    ))
  )

  val publicFields = PublicCreditCardOptionalFields(
    holderId = None, holderName = Some("someHolderName"), billingAddressDetailed = Some(billingAddress)
  )

  val optionalFields = CreditCardOptionalFields(
    csc = Some("123"), publicFields = Some(publicFields)
  )

  val creditCard = CreditCard("4580458045804580", YearMonth(2020, 12), Some(optionalFields))

  val customer = Customer(
    phone = Some("somePhone"),
    email = Some("someEmail")
  )

  val deal = Deal(
    id = "someDeal",
    invoiceId = Some("someInvoiceId"),
    shippingAddress = Some(shippingAddress)
  )

  val currencyAmount = CurrencyAmount("USD", 5.67)
  val payment = Payment(currencyAmount, installments = 1)

  def beRejectedWithMessage(message: String): Matcher[Try[String]] = beFailedTry.like { case e: PaymentRejectedException => e.message mustEqual message }
  def failWithMessage(message: String): Matcher[Try[String]] = beFailedTry.like { case e: PaymentErrorException => e.message must contain(message) }

  def beParseError: Matcher[Try[String]] = failWithCause[ParseException]
  def beUnsupportedError: Matcher[Try[String]] = failWithCause[UnsupportedOperationException]
  private def failWithCause[T : ClassTag]: Matcher[Try[String]] = beFailedTry.like { case e: PaymentErrorException => e.cause must beAnInstanceOf[T] }
  
  implicit class CreditCardTestExtensions(o: CreditCard) {
    def withCsc(csc: String) = withOptionalFields(_.withCsc(csc))
    def withoutCsc = withOptionalFields(_.withoutCsc)

    def withHolderName(holderName: String) = withOptionalFields(_.withHolderName(holderName))
    def withoutHolderName = withOptionalFields(_.withoutHolderName)

    def withoutBillingAddress = withOptionalFields(_.withoutBillingAddress)
    def withBillingAddress(f: AddressDetailed => AddressDetailed) = withOptionalFields(_.withBillingAddress(f))
    
    private def withOptionalFields(f: CreditCardOptionalFields => CreditCardOptionalFields): CreditCard =
      o.copy(additionalFields = Some(f(o.additionalFields.get)))
  }
  
  implicit class CreditCardOptionalFieldsTestExtensions(o: CreditCardOptionalFields) {
    def withCsc(csc: String) = o.copy(csc = Some(csc))
    def withoutCsc = o.copy(csc = None)

    def withHolderName(holderName: String) = withPublicFields(_.withHolderName(holderName))
    def withoutHolderName = withPublicFields(_.withoutHolderName)

    def withoutBillingAddress = withPublicFields(_.withoutBillingAddress)
    def withBillingAddress(f: AddressDetailed => AddressDetailed) = withPublicFields(_.withBillingAddress(f))
    
    private def withPublicFields(f: PublicCreditCardOptionalFields => PublicCreditCardOptionalFields): CreditCardOptionalFields =
      o.copy(publicFields = Some(f(o.publicFields.get)))
  }
  
  implicit class PublicCreditCardOptionalFieldsTestExtensions(o: PublicCreditCardOptionalFields) {
    def withHolderName(holderName: String) = o.copy(holderName = Some(holderName))
    def withoutHolderName = o.copy(holderName = None)

    def withoutBillingAddress = o.copy(billingAddressDetailed = None)
    def withBillingAddress(f: AddressDetailed => AddressDetailed) = o.copy(billingAddressDetailed = Some(f(o.billingAddressDetailed.get)))
  }
  
  implicit class CustomerTestExtensions(o: Customer) {
    def withPhone(phone: String) = o.copy(phone = Some(phone))
    def withoutPhone = o.copy(phone = None)
    
    def withEmail(email: String) = o.copy(email = Some(email))
    def withoutEmail = o.copy(email = None)
  }

  implicit class DealTestExtensions(o: Deal) {
    def withInvoiceId(invoiceId: String) = o.copy(invoiceId = Some(invoiceId))
    def withoutInvoiceId = o.copy(invoiceId = None)

    def withoutShippingAddress = o.copy(shippingAddress = None)
    def withShippingAddress(f: ShippingAddress => ShippingAddress): Deal =
      o.copy(shippingAddress = Some(f(o.shippingAddress.get)))
  }
  
  implicit class ShippingAddressTestExtensions(o: ShippingAddress) {
    def withFirstName(firstName: String) = o.copy(firstName = Some(firstName))
    def withoutFirstName = o.copy(firstName = None)
    
    def withLastName(lastName: String) = o.copy(lastName = Some(lastName))
    def withoutLastName = o.copy(lastName = None)

    def withoutName = withoutFirstName.withoutLastName

    def withStreet(street: String) = withAddress(_.withStreet(street))
    def withoutStreet = withAddress(_.withoutStreet)

    def withCity(city: String) = withAddress(_.withCity(city))
    def withoutCity = withAddress(_.withoutCity)

    def withState(state: String) = withAddress(_.withState(state))
    def withoutState = withAddress(_.withoutState)

    def withPostalCode(postalCode: String) = withAddress(_.withPostalCode(postalCode))
    def withoutPostalCode = withAddress(_.withoutPostalCode)

    def withCountryCode(countryCode: Locale) = withAddress(_.withCountryCode(countryCode))
    def withoutCountryCode = withAddress(_.withoutCountryCode)

    private def withAddress(f: AddressDetailed => AddressDetailed) = o.copy(address = Some(f(o.address.get)))
  }

  implicit class AddressDetailedTestExtensions(o: AddressDetailed) {
    def withStreet(street: String) = o.copy(street = Some(street))
    def withoutStreet = o.copy(street = None)

    def withCity(city: String) = o.copy(city = Some(city))
    def withoutCity = o.copy(city = None)

    def withState(state: String) = o.copy(state = Some(state))
    def withoutState = o.copy(state = None)

    def withPostalCode(postalCode: String) = o.copy(postalCode = Some(postalCode))
    def withoutPostalCode = o.copy(postalCode = None)

    def withCountryCode(countryCode: Locale) = o.copy(countryCode = Some(countryCode))
    def withoutCountryCode = o.copy(countryCode = None)
  }
  
  implicit class PaymentTestExtensions(o: Payment) {
    def withInstallments(installments: Int) = o.copy(installments = installments)
  }
}
