package com.wix.pay.twocheckout

import com.wix.pay.creditcard.CreditCard
import com.wix.pay.model.{Customer, Deal, Payment}
import org.specs2.mutable.SpecWithJUnit
import org.specs2.specification.Scope

class TwocheckoutParamsValidatorTest extends SpecWithJUnit with TwocheckoutTestSupport {
  "TwocheckoutParamsValidator" should {
    "succeed if all parameters pass validation" in new Ctx {
      validate() must not(throwAn[Exception])
    }

    "fail if payment has more than 1 installment" in new Ctx {
      validate(withPayment = payment.withInstallments(2)) must
        failWithMessage("2Checkout does not support installments!")
    }

    "fail if customer is not provided" in new Ctx {
      validate(withCustomer = None) must failWithMessage("Customer is mandatory for 2Checkout!")
    }

    "fail if customer phone is not provided" in new Ctx {
      validate(withCustomer = Some(customer.withoutPhone)) must
        failWithMessage("Customer phone is mandatory for 2Checkout!")
    }

    "fail if customer phone is empty" in new Ctx {
      validate(withCustomer = Some(customer.withPhone(""))) must
        failWithMessage("Customer phone is mandatory for 2Checkout!")
    }

    "fail if customer email is not provided" in new Ctx {
      validate(withCustomer = Some(customer.withoutEmail)) must
        failWithMessage("Customer email is mandatory for 2Checkout!")
    }

    "fail if customer email is empty" in new Ctx {
      validate(withCustomer = Some(customer.withEmail(""))) must
        failWithMessage("Customer email is mandatory for 2Checkout!")
    }

    "fail if credit card csc is not provided" in new Ctx {
      validate(withCreditCard = creditCard.withoutCsc) must
        failWithMessage("Credit Card CSC is mandatory for 2Checkout!")
    }

    "fail if credit card csc is empty" in new Ctx {
      validate(withCreditCard = creditCard.withCsc("")) must
        failWithMessage("Credit Card CSC is mandatory for 2Checkout!")
    }

    "fail if credit card holder name is not provided" in new Ctx {
      validate(withCreditCard = creditCard.withoutHolderName) must
        failWithMessage("Credit Card holder name is mandatory for 2Checkout!")
    }

    "fail if credit card holder name is empty" in new Ctx {
      validate(withCreditCard = creditCard.withHolderName("")) must
        failWithMessage("Credit Card holder name is mandatory for 2Checkout!")
    }

    "fail if credit card billing address is not provided" in new Ctx {
      validate(withCreditCard = creditCard.withoutBillingAddress) must
        failWithMessage("Credit Card billing address is mandatory for 2Checkout!")
    }

    "fail if credit card billing street is not provided" in new Ctx {
      validate(withCreditCard = creditCard.withBillingAddress(_.withoutStreet)) must
        failWithMessage("Credit Card billing street is mandatory for 2Checkout!")
    }

    "fail if credit card billing street is empty" in new Ctx {
      validate(withCreditCard = creditCard.withBillingAddress(_.withStreet(""))) must
        failWithMessage("Credit Card billing street is mandatory for 2Checkout!")
    }

    "fail if credit card billing city is not provided" in new Ctx {
      validate(withCreditCard = creditCard.withBillingAddress(_.withoutCity)) must
        failWithMessage("Credit Card billing city is mandatory for 2Checkout!")
    }

    "fail if credit card billing city is empty" in new Ctx {
      validate(withCreditCard = creditCard.withBillingAddress(_.withCity(""))) must
        failWithMessage("Credit Card billing city is mandatory for 2Checkout!")
    }

    "fail if credit card billing state is not provided" in new Ctx {
      validate(withCreditCard = creditCard.withBillingAddress(_.withoutState)) must
        failWithMessage("Credit Card billing state is mandatory for 2Checkout!")
    }

    "fail if credit card billing state is empty" in new Ctx {
      validate(withCreditCard = creditCard.withBillingAddress(_.withState(""))) must
        failWithMessage("Credit Card billing state is mandatory for 2Checkout!")
    }

    "fail if credit card billing postalCode is not provided" in new Ctx {
      validate(withCreditCard = creditCard.withBillingAddress(_.withoutPostalCode)) must
        failWithMessage("Credit Card billing postal code is mandatory for 2Checkout!")
    }

    "fail if credit card billing postalCode is empty" in new Ctx {
      validate(withCreditCard = creditCard.withBillingAddress(_.withPostalCode(""))) must
        failWithMessage("Credit Card billing postal code is mandatory for 2Checkout!")
    }

    "fail if credit card billing countryCode is not provided" in new Ctx {
      validate(withCreditCard = creditCard.withBillingAddress(_.withoutCountryCode)) must
        failWithMessage("Credit Card billing country is mandatory for 2Checkout!")
    }

    "fail if deal is not provided" in new Ctx {
      validate(withDeal = None) must failWithMessage("Deal is mandatory for 2Checkout!")
    }

    "fail if deal invoiceId is not provided" in new Ctx {
      validate(withDeal = Some(deal.withoutInvoiceId)) must
        failWithMessage("Deal invoiceId is mandatory for 2Checkout!")
    }

    "fail if deal invoiceId is empty" in new Ctx {
      validate(withDeal = Some(deal.withInvoiceId(""))) must
        failWithMessage("Deal invoiceId is mandatory for 2Checkout!")
    }

    "fail if both deal shipping firstName & lastName are not provided" in new Ctx {
      validate(withDeal = Some(deal.withShippingAddress(_.withoutFirstName.withoutLastName))) must
        failWithMessage("Deal shipping name is mandatory for 2Checkout!")
    }

    "fail if both deal shipping firstName & lastName are empty" in new Ctx {
      validate(withDeal = Some(deal.withShippingAddress(_.withFirstName("").withLastName("")))) must
        failWithMessage("Deal shipping name is mandatory for 2Checkout!")
    }

    "fail if both deal shipping firstName & lastName are all whitespace" in new Ctx {
      validate(withDeal = Some(deal.withShippingAddress(_.withFirstName(" ").withLastName("  ")))) must
        failWithMessage("Deal shipping name is mandatory for 2Checkout!")
    }

    "fail if deal shipping address is not provided" in new Ctx {
      validate(withDeal = Some(deal.withoutShippingAddress)) must
        failWithMessage("Deal shipping address is mandatory for 2Checkout!")
    }

    "fail if deal shipping street is not provided" in new Ctx {
      validate(withDeal = Some(deal.withShippingAddress(_.withoutStreet))) must
        failWithMessage("Deal shipping street is mandatory for 2Checkout!")
    }

    "fail if deal shipping street is empty" in new Ctx {
      validate(withDeal = Some(deal.withShippingAddress(_.withStreet("")))) must
        failWithMessage("Deal shipping street is mandatory for 2Checkout!")
    }

    "fail if deal shipping city is not provided" in new Ctx {
      validate(withDeal = Some(deal.withShippingAddress(_.withoutCity))) must
        failWithMessage("Deal shipping city is mandatory for 2Checkout!")
    }

    "fail if deal shipping city is empty" in new Ctx {
      validate(withDeal = Some(deal.withShippingAddress(_.withCity("")))) must
        failWithMessage("Deal shipping city is mandatory for 2Checkout!")
    }

    "fail if deal shipping state is not provided" in new Ctx {
      validate(withDeal = Some(deal.withShippingAddress(_.withoutState))) must
        failWithMessage("Deal shipping state is mandatory for 2Checkout!")
    }

    "fail if deal shipping state is empty" in new Ctx {
      validate(withDeal = Some(deal.withShippingAddress(_.withState("")))) must
        failWithMessage("Deal shipping state is mandatory for 2Checkout!")
    }

    "fail if deal shipping postalCode is not provided" in new Ctx {
      validate(withDeal = Some(deal.withShippingAddress(_.withoutPostalCode))) must
        failWithMessage("Deal shipping postal code is mandatory for 2Checkout!")
    }

    "fail if deal shipping postalCode is empty" in new Ctx {
      validate(withDeal = Some(deal.withShippingAddress(_.withPostalCode("")))) must
        failWithMessage("Deal shipping postal code is mandatory for 2Checkout!")
    }

    "fail if deal shipping countryCode is not provided" in new Ctx {
      validate(withDeal = Some(deal.withShippingAddress(_.withoutCountryCode))) must
        failWithMessage("Deal shipping country is mandatory for 2Checkout!")
    }
  }

  trait Ctx extends Scope {
    def validate(withCreditCard: CreditCard = creditCard,
                 withPayment: Payment = payment,
                 withCustomer: Option[Customer] = Some(customer),
                 withDeal: Option[Deal] = Some(deal)) =
      TwocheckoutParamsValidator.validate(withCreditCard, withPayment, withCustomer, withDeal)

    def failWithMessage(message: String) = throwA[IllegalArgumentException].like { case e: IllegalArgumentException => e.getMessage must contain(message) }
  }
}
