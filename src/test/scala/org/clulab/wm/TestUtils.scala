package org.clulab.wm

import org.clulab.odin.{Attachment, Mention}
import org.scalatest._

/**
  * These are the functions that we'll be testing, that import from PaperReader
  */

//val eeWithActionsAndGlobal = ExtractorEngine(rules, myActions, myGlobalAction)
object TestUtils {
  
  // This will be a GraphTest in contrast to a RuleTest
  class Test extends FlatSpec with Matchers {
    class TesterTag extends Tag("org.clulab.wm.TestUtils")
    
    object Nobody   extends TesterTag
    object Somebody extends TesterTag
    object Keith    extends TesterTag
    object Becky    extends TesterTag
    object Egoitz   extends TesterTag
    object Ajay     extends TesterTag
    object Adarsh   extends TesterTag
    object Mithun   extends TesterTag
    object Fan      extends TesterTag
    object Zheng    extends TesterTag
    object Mihai    extends TesterTag
    object Ben      extends TesterTag

    
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
      new Quantification(quantifier, None)
  def newQuantification(quantifier: String, adverbs: String*) =
    new Quantification(quantifier, Option(adverbs.toSeq))

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
