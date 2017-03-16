package com.wix.pay.twocheckout.model

case class TwocheckoutSettings(production: TwocheckoutEnvironment, sandbox: TwocheckoutEnvironment) {
  def endpointUrl(sandboxMode: Boolean) = if (sandboxMode) sandbox.endpointUrl else production.endpointUrl
  def jsSdkUrl(sandboxMode: Boolean) = if (sandboxMode) sandbox.jsSdkUrl else production.jsSdkUrl
  def environment(sandboxMode: Boolean) = if (sandboxMode) Environments.sandbox else Environments.production
}

object TwocheckoutSettings {
  def apply(env: TwocheckoutEnvironment): TwocheckoutSettings = TwocheckoutSettings(env, env)
}

case class TwocheckoutEnvironment(endpointUrl: String, jsSdkUrl: String)