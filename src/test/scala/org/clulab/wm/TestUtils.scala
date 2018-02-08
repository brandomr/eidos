package org.clulab.wm

import org.scalatest._

import org.clulab.odin.Attachment
import org.clulab.odin.Mention

import org.clulab.wm.Aliases.Quantifier

/**
  * These are the functions that we'll be testing, that import from PaperReader
  */

//val eeWithActionsAndGlobal = ExtractorEngine(rules, myActions, myGlobalAction)
object TestUtils {
  
  // This will be a GraphTest in contrast to a RuleTest
  class Test extends FlatSpec with Matchers {
    protected val tagName = "org.clulab.wm.TestUtils"
    object Nobody extends Tag(tagName)
    object Somebody extends Tag(tagName)
    object Keith extends Tag(tagName)
    object Becky extends Tag(tagName)
    object Egoitz extends Tag(tagName)
    object Ajay extends Tag(tagName)
    object Adarsh extends Tag(tagName)
    object Mithun extends Tag(tagName)
    object Fan extends Tag(tagName)
    object Zheng extends Tag(tagName)
    object Mihai extends Tag(tagName)
    object Ben extends Tag(tagName)
    object Heather extends Tag(tagName)


    val passingTest = it
    val failingTest = ignore
    
    val successful = Seq()
    
    class Tester(text: String) {
      val mentions = extractMentions(text)
      
      protected def toString(mentions: Seq[Mention]): String = {
        val stringBuilder = new StringBuilder()
        
        mentions.indices.foreach(index => stringBuilder.append(s"${index}: ${mentions(index).text}\n"))
        stringBuilder.toString()
      }
    
      protected def annotateTest(result: Seq[String]): Seq[String] =
          if (result == successful)
            result
          else
            result ++ Seq("Mentions:\n" + toString(mentions))
      
      def test(nodeSpec: NodeSpec): Seq[String] = annotateTest(nodeSpec.test(mentions))
      
      def test(edgeSpec: EdgeSpec): Seq[String] = annotateTest(edgeSpec.test(mentions))
    }
  }
  
  protected lazy val system = new AgroSystem() // TODO: Change this class name

  def extractMentions(text: String): Seq[Mention] = system.extractFrom(text)
  
  def newNodeSpec(nodeText: String, attachments: Set[Attachment]) =
      new NodeSpec(nodeText, attachments)
  def newNodeSpec(nodeText: String, attachments: Attachment*) =
      new NodeSpec(nodeText, attachments.toSet)
  
  def newEdgeSpec(cause: NodeSpec, event: Event, effects: NodeSpec*) =
      new EdgeSpec(cause, event, effects.toSet)
  
  
  def newQuantification(quantifier: String) =
      new Quantification(quantifier)
  
  def newDecrease(trigger: String) =
      new Decrease(trigger, None)
  def newDecrease(trigger: String, quantifiers: String*) =
      new Decrease(trigger, Option(quantifiers.toSeq))
          
  def newIncrease(trigger: String) =
      new Increase(trigger, None)
  def newIncrease(trigger: String, quantifiers: String*) =
      new Increase(trigger, Option(quantifiers.toSeq))

  def newUnmarked(quantifier: String) =
      new Unmarked(quantifier)
}
