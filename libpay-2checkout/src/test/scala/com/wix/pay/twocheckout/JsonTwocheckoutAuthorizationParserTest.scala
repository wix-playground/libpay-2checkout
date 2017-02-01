package com.wix.pay.twocheckout


import org.specs2.mutable.SpecWithJUnit
import org.specs2.specification.Scope


class JsonTwocheckoutAuthorizationParserTest extends SpecWithJUnit {
  trait Ctx extends Scope {
    val parser: TwocheckoutAuthorizationParser = new JsonTwocheckoutAuthorizationParser
  }

  "stringify and then parse" should {
    "yield a merchant similar to the original one" in new Ctx {
      val someAuthorization = TwocheckoutAuthorization()

      val authorizationKey = parser.stringify(someAuthorization)
      parser.parse(authorizationKey) must beEqualTo(someAuthorization)
    }
  }
}