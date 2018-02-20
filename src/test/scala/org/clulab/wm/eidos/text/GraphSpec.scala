package org.clulab.wm.eidos.text

import scala.collection.Seq

import org.clulab.odin.Attachment
import org.clulab.odin.EventMention
import org.clulab.odin.Mention
import org.clulab.odin.TextBoundMention
import org.clulab.wm.eidos.Aliases.Quantifier
import org.clulab.wm.eidos.Decrease
import org.clulab.wm.eidos.Increase
import org.clulab.wm.eidos.Quantification

case class Unmodified(quantifier: Quantifier) extends Attachment

abstract class GraphSpec

class EventSpec(val label: String) extends GraphSpec

object NoEvent extends EventSpec("")
object Causal extends EventSpec("Causal")
object Correlation extends EventSpec("Correlation")
object IsA extends EventSpec("IsA")
object Origin extends EventSpec("Origin")
object TransparentLink extends EventSpec("TransparentLink")
object Affect extends EventSpec("Affect")

class AttachmentSpec(val attachment: Attachment) extends GraphSpec {
  protected def toString(quantifiers: Option[Seq[Quantifier]]): String = {
    val stringBuilder = new StringBuilder()
    
    if (quantifiers != None)
      stringBuilder
          .append(", ")
          .append(quantifiers.get.map("Quant: " + _).mkString(", "))
    stringBuilder.toString()
  }  
}

class Quant(quantization: Quantification) extends AttachmentSpec(quantization) {
  override def toString = "+QUANT(" + quantization.quantifier + ")"
}

object Quant {
  def apply(quantifier: Quantifier) =
      new Quant(Quantification(quantifier, None))

  def apply(quantifier: Quantifier, adverbs: String*): Quant = new Quant(Quantification(quantifier, Option(adverbs.toSeq)))
}

class Dec(decrease: Decrease) extends AttachmentSpec(decrease) {
  override def toString = "+DEC(" + decrease.trigger + toString(decrease.quantifier) + ")"  
}

object Dec {
  def apply(trigger: String) =
      new Dec(Decrease(trigger, None))
  
  def apply(trigger: String, quantifiers: String*) =
      new Dec(Decrease(trigger, Option(quantifiers.toSeq)))
}

class Inc(increase: Increase) extends AttachmentSpec(increase) {
  override def toString = "+INC(" + increase.trigger + toString(increase.quantifier) + ")"
}

object Inc {
  def apply(trigger: String) =
    new Inc(Increase(trigger, None))
  
  def apply(trigger: String, quantifiers: String*) =
      new Inc(Increase(trigger, Option(quantifiers.toSeq)))
}    

class Unmarked(unmodified: Unmodified) extends AttachmentSpec(unmodified) {
  override def toString = "+" + unmodified.quantifier
}

object Unmarked {
  def apply(quantifier: Quantifier) =
      new Unmarked(Unmodified(quantifier))
}

class NodeSpec(val nodeText: String, val attachmentSpecs: Set[AttachmentSpec]) extends GraphSpec {
  val attachments = attachmentSpecs.map(_.attachment)
  var mention: Option[Mention] = None
  var tested = false
  var complaints = Seq[String]()
  
  protected def matchAttachments(mention: TextBoundMention): Boolean = {
    val success = mention.attachments == attachments
    
    success
  }

  protected def matchText(mention: TextBoundMention): Boolean = {
    val text = mention.text
    val success = text == nodeText
    
    success
  }
    
  protected def testSpec(mentions: Seq[Mention]): Option[Mention] = {
    val matches = mentions
        .filter(_.isInstanceOf[TextBoundMention])
        .map(_.asInstanceOf[TextBoundMention])
        .filter(matchText(_))
        .filter(matchAttachments(_))
        
    if (matches.size == 1) Option(matches.head)
    else None
  }
  
  def test(mentions: Seq[Mention]): Seq[String] = {
    if (!tested) {
      mention = testSpec(mentions)
      if (mention == None)
        complaints = Seq("Could not find NodeSpec " + this)
      tested = true
    }
    complaints
  }
  
  override def toString(): String = {
    val stringBuilder = new StringBuilder("[")
        .append(nodeText)
        .append(if (!attachments.isEmpty) "|" else "")
        
    attachmentSpecs.foreach(attachmentSpec => stringBuilder.append(attachmentSpec.toString))
    stringBuilder
        .append("]")
        .toString()
  }
}

object NodeSpec {
  def apply(nodeText: String, attachmentSpecs: Set[AttachmentSpec]) =
      new NodeSpec(nodeText, attachmentSpecs)
  def apply(nodeText: String, attachmentSpecs: AttachmentSpec*) =
      new NodeSpec(nodeText, attachmentSpecs.toSet)  
}

class EdgeSpec(val cause: NodeSpec, val event: EventSpec, val effects: Set[NodeSpec]) extends GraphSpec {
  protected def testPattern = testLines(_)
  //protected def testPattern = testStar(_)
  
  protected def getArgument(mention: EventMention, nodeSpec: NodeSpec, argument: String): Option[Mention] = {
    val tmpMention = nodeSpec.mention.get

    if (mention.arguments.contains(argument))
      mention.arguments(argument).find(_ == tmpMention)
    else 
      None
  }
    
  protected def getCause(mention: EventMention): Option[Mention] =
      getArgument(mention, cause, "cause")

  protected def getEffect(mention: EventMention, effect: NodeSpec): Option[Mention] =
      getArgument(mention, effect, "effect")
  
  protected def matchCause(mention: EventMention): Boolean =
      getArgument(mention, cause, "cause") != None

  protected def matchEffect(mention: EventMention, effect: NodeSpec) =
      getArgument(mention, effect, "effect") != None
    
  protected def matchEffect(mention: EventMention): Boolean =
      effects.exists(matchEffect(mention, _))
        
  protected def matchEffects(mention: EventMention): Boolean = { 
    val tmpEffects = effects.map(_.mention.get) 
 
    if (mention.arguments.contains("effect")) 
      // This has to be all of them at once and only all of them
      // Both are converted to sets for comparison
      // If the effects can be a subset, use .sameElements(tmpEfects)
      mention.arguments("effect").toSet == tmpEffects
    else  
      false 
  }
      
  protected def testSpec(mentions: Seq[Mention]): Option[Mention] = {
    val matches = mentions
        .filter(_.isInstanceOf[EventMention])
        .map(_.asInstanceOf[EventMention])
        .filter(_.labels.contains(event.label))
        .filter(matchCause(_))
        .filter(matchEffects(_)) // All of them
    
    if (matches.size == 1) Option(matches.head)
    else None
  }
    
  protected def testLines(mentions: Seq[Mention]): Seq[String] = {
    val matches = mentions
        .filter(_.isInstanceOf[EventMention])
        .map(_.asInstanceOf[EventMention])
        .filter(_.labels.contains(event.label))
        .filter(matchCause)
        .filter(matchEffect(_)) // One of them
    val badCause = matches.find(mention => getCause(mention).get != cause.mention.get).isDefined
    val effectResults = effects.toSeq
        .map(effect => (effect, matches.find(mention => matchEffect(mention, effect))))
    val complaints = effectResults.flatMap(effectResult =>
      if (effectResult._2.isDefined) Seq()
      else Seq("Could not find line EdgeSpec " + effectResult._1)
    )
    
    if (badCause) complaints ++ Seq("Not all effects had same cause")
    else complaints
  }
    
  protected def testStar(mentions: Seq[Mention]): Seq[String] =
    if (testSpec(mentions) == None)
      Seq("Could not find star EdgeSpec " + this)
    else 
      Seq()
      
  def test(mentions: Seq[Mention]): Seq[String] = {
    val causeComplaints = cause.test(mentions)
    val effectComplaints = effects.flatMap(_.test(mentions))

    val causeSuccess = causeComplaints.isEmpty
    val effectSuccess = effectComplaints.isEmpty
    
    val edgeComplaints =
        if (causeSuccess && effectSuccess) testPattern(mentions)
        else Seq()            

    causeComplaints ++ effectComplaints ++ edgeComplaints
  }
  
  override def toString(): String = {
    new StringBuilder(cause.toString())
        .append("->(")
        .append(event.label)
        .append(")->")
        .append(
            if (effects.isEmpty)
              "NoEdge"
            else
              effects.map(_.toString()).mkString("->")
        )
        .toString()
  }
}

object EdgeSpec {
    def apply(cause: NodeSpec, event: EventSpec, effects: NodeSpec*) =
      new EdgeSpec(cause, event, effects.toSet)
}