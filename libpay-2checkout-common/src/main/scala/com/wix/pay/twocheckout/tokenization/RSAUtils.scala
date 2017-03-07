package com.wix.pay.twocheckout.tokenization

import java.security.KeyFactory
import java.security.spec.RSAPublicKeySpec
import javax.crypto.Cipher

import org.apache.commons.codec.binary.Base64

object RSAUtils {

  def rsaEncrypt(key: RSAPublicKey, text: String) = {
    val modulus = BigInt(1, Base64.decodeBase64(key.base64Modulus))
    val exp = BigInt(1, Base64.decodeBase64(key.base64Exp))

    val publicKeySpec = new RSAPublicKeySpec(modulus.bigInteger, exp.bigInteger)
    val publicKey = KeyFactory.getInstance("RSA").generatePublic(publicKeySpec)

    val cipher = Cipher.getInstance("RSA/ECB/PKCS1PADDING")
    cipher.init(Cipher.ENCRYPT_MODE, publicKey)
    val res = cipher.doFinal(text.getBytes("UTF-8"))
    Base64.encodeBase64String(res)
  }
}

case class RSAPublicKey(base64Modulus: String, base64Exp: String)