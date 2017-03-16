package com.wix.pay.twocheckout


import org.specs2.mutable.SpecWithJUnit
import org.specs2.specification.Scope


class JsonTwocheckoutMerchantParserTest extends SpecWithJUnit {
  trait Ctx extends Scope {
    val parser: TwocheckoutMerchantParser = JsonTwocheckoutMerchantParser
  }

  "stringify and then parse" should {
    "yield a merchant similar to the original one" in new Ctx {
      val someMerchant = TwocheckoutMerchant(
        sellerId = "some seller ID",
        publishableKey = "some publishable key",
        privateKey = "some private key",
        sandboxMode = true
      )

      val merchantKey = parser.stringify(someMerchant)
      parser.parse(merchantKey) must beEqualTo(someMerchant)
    }

    "parse credentials without explicit mode" in new Ctx {
      val someMerchantStr = """{"sellerId":"sellerId","publishableKey":"publishableKey","privateKey":"privateKey"}"""
      val someMerchant = TwocheckoutMerchant(
        sellerId = "sellerId",
        publishableKey = "publishableKey",
        privateKey = "privateKey",
        sandboxMode = false
      )

      parser.parse(someMerchantStr) must beEqualTo(someMerchant)
    }
  }
}