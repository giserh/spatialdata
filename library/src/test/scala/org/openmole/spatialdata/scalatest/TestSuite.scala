package org.openmole.spatialdata.scalatest

import org.openmole.spatialdata.scalatest.network.NetworkSuite
import org.openmole.spatialdata.scalatest.utils.UtilsSuite
import org.scalatest.Suites

class TestSuite extends Suites(
  new NetworkSuite,
  new UtilsSuite
)