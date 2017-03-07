package com.wix.pay.twocheckout.tokenization

import java.security.spec.RSAPublicKeySpec
import java.security.{KeyFactory, KeyPairGenerator}
import javax.crypto.Cipher

import org.apache.commons.codec.binary.Base64
import org.specs2.mutable.SpecWithJUnit
import org.specs2.specification.Scope

class RSAUtilsTest extends SpecWithJUnit {

  "RSAUtils" should {

    "encrypt data with provided key in base64 format" in new Ctx {
      val encrypted = RSAUtils.rsaEncrypt(publicKey, someMessage)
      decryptBase64(encrypted) mustEqual someMessage
    }

    "encrypt empty string with provided key in base64 format" in new Ctx {
      val encrypted = RSAUtils.rsaEncrypt(publicKey, emptyMessage)
      decryptBase64(encrypted) mustEqual emptyMessage
    }
  }

  trait Ctx extends Scope {
    val factory = KeyPairGenerator.getInstance("RSA")
    factory.initialize(2048)

    val keys = factory.genKeyPair()
    val publicKeySpec = KeyFactory.getInstance("RSA").getKeySpec(keys.getPublic, classOf[RSAPublicKeySpec])
    val publicKey = RSAPublicKey(
      Base64.encodeBase64String(publicKeySpec.getModulus.toByteArray),
      Base64.encodeBase64String(publicKeySpec.getPublicExponent.toByteArray)
    )
    println(publicKey)
    val someMessage = "some message hello"
    val emptyMessage = ""

    def decryptBase64(encrypted: String): String = {
      val bytes = Base64.decodeBase64(encrypted)
      val cipher = Cipher.getInstance("RSA/ECB/PKCS1PADDING")
      cipher.init(Cipher.DECRYPT_MODE, keys.getPrivate)
      new String(cipher.doFinal(bytes), "utf-8")
    }
  }

}
