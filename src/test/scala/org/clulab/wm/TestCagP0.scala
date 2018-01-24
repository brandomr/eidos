package org.clulab.wm

import CAG._
import TestUtils._

class TestCagP0 extends AgroTest {
  val mentions = extractMentions(p0s1)

  "p0s1" should "not be ignored" in {
  }
  
  it should "have the correct triples" taggedAs(Keith) in {
    // Should this just be "rainfall"?
    val rainfallNode = newNodeSpec("in rainfall", newDecrease("decrease"))
    val povertyNode = newNodeSpec("poverty", newIncrease("increased", "significantly"))
    
    val rainfallPovertyEdge = newEdgeSpec(rainfallNode, povertyNode)
    
    rainfallPovertyEdge.test(mentions) shouldBe successful
  }
  
  ignore should "be ignored" taggedAs(Becky) in {
    6 should be (5)
  }
  
  ignore should "still be ignored" taggedAs(Becky, Keith) in {
    5 should be (6)
  }
}