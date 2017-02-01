package com.wix.pay.twocheckout.testkit

import com.wix.hoopoe.http.testkit.EmbeddedHttpProbe

class TwocheckoutDriver(port: Int) {
  val probe = new EmbeddedHttpProbe(port, EmbeddedHttpProbe.NotFoundHandler)

  def start() {
    probe.doStart()
  }

  def stop() {
    probe.doStop()
  }

  def reset() {
    probe.reset()
  }
}
