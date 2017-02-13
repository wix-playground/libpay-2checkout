package com.wix.pay.twocheckout

import com.wix.pay.creditcard.{AddressDetailed, CreditCard}
import com.wix.pay.model.{Customer, Deal, Payment}

trait TwocheckoutParamsValidator {
  def validate(creditCard: CreditCard, payment: Payment, customer: Option[Customer], deal: Option[Deal]): Unit
}

object TwocheckoutParamsValidator extends TwocheckoutParamsValidator {
  override def validate(creditCard: CreditCard, payment: Payment, customer: Option[Customer], deal: Option[Deal]): Unit = {
    require(payment.installments == 1, "2Checkout does not support installments!")

    nonEmpty(creditCard.csc, "Credit Card CSC")
    nonEmpty(creditCard.holderName, "Credit Card holder name")
    validateAddress(creditCard.billingAddressDetailed, "Credit Card billing")
    isDefined(customer, "Customer")
    nonEmpty(customer.get.phone, "Customer phone")
    nonEmpty(customer.get.email, "Customer email")

    isDefined(deal, "Deal")
    nonEmpty(deal.get.invoiceId, "Deal invoiceId")
    isDefined(deal.get.shippingAddress, "Deal shipping address")
    val shippingAddress = deal.get.shippingAddress.get
    require(shippingAddress.firstName.exists(_.trim.nonEmpty) || shippingAddress.lastName.exists(_.trim.nonEmpty),
      "Deal shipping name is mandatory for 2Checkout!"
    )
    validateAddress(shippingAddress.address, "Deal shipping")
  }

  private def validateAddress(addressDetailed: Option[AddressDetailed], name: => String): Unit = {
    require(addressDetailed.isDefined, s"$name address is mandatory for 2Checkout!")
    val address = addressDetailed.get
    nonEmpty(address.street, s"$name street")
    nonEmpty(address.city, s"$name city")
    nonEmpty(address.postalCode, s"$name postal code")
    isDefined(address.countryCode, s"$name country")
    nonEmpty(address.state, s"$name state")
  }

  private def isDefined(field: Option[Any], name: => String): Unit = {
    require(field.isDefined, s"$name is mandatory for 2Checkout!")
  }

  private def nonEmpty(field: Option[String], name: => String): Unit = {
    require(field.exists(_.nonEmpty), s"$name is mandatory for 2Checkout!")
  }
}
