package org.jetbrains.plugins.scala
package lang
package parser
package parsing
package patterns

import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.parser.parsing.builder.ScalaPsiBuilder
import org.jetbrains.plugins.scala.lang.parser.util.ParserUtils

/**
* @author Alexander Podkhalyuzin
* Date: 29.02.2008
*/
object Patterns extends Patterns {
  override protected def pattern = Pattern
}

trait Patterns {
  protected def pattern: Pattern

  def parse(builder: ScalaPsiBuilder): Boolean = parse(builder,underParams = false)
  def parse(builder: ScalaPsiBuilder, underParams: Boolean): Boolean = {
    val patternsMarker = builder.mark
    if (!pattern.parse(builder)) {
      builder.getTokenType match {
        case ScalaTokenTypes.tUNDER =>
          builder.advanceLexer()
          builder.getTokenText match {
            case "*" =>
              builder.advanceLexer()
              patternsMarker.done(ScalaElementTypes.SEQ_WILDCARD)
              return true
            case _ =>
          }
        case _ =>
      }
      patternsMarker.rollbackTo()
      return false
    }
    builder.getTokenType match {
      case ScalaTokenTypes.tCOMMA =>
        builder.advanceLexer() //Ate ,
      var end = false
        while ((!end || !underParams) && pattern.parse(builder)) {
          builder.getTokenType match {
            case ScalaTokenTypes.tCOMMA =>
              builder.advanceLexer() //Ate ,
              if (ParserUtils.eatSeqWildcardNext(builder) && underParams) end = true
            case _ =>
              patternsMarker.done(ScalaElementTypes.PATTERNS)
              return true
          }
        }
        if (underParams) {
          ParserUtils.eatSeqWildcardNext(builder)
        }
        patternsMarker.done(ScalaElementTypes.PATTERNS)
        true
      case _ =>
        patternsMarker.rollbackTo()
        false
    }
  }
}

object XmlPatterns extends ParserNode {
  def parse(builder: ScalaPsiBuilder): Boolean = {
    val args = builder.mark
    def parseSeqWildcard(withComma: Boolean): Boolean = {
      if (if (withComma)
        lookAhead(builder, ScalaTokenTypes.tCOMMA, ScalaTokenTypes.tUNDER, ScalaTokenTypes.tIDENTIFIER)
      else lookAhead(builder, ScalaTokenTypes.tUNDER, ScalaTokenTypes.tIDENTIFIER)) {
        if (withComma) builder.advanceLexer()
        val wild = builder.mark
        builder.getTokenType
        builder.advanceLexer()
        if (builder.getTokenType == ScalaTokenTypes.tIDENTIFIER && "*".equals(builder.getTokenText)) {
          builder.advanceLexer()
          wild.done(ScalaElementTypes.SEQ_WILDCARD)
          true
        } else {
          wild.rollbackTo()
          false
        }
      } else {
        false
      }
    }

    def parseSeqWildcardBinding(withComma: Boolean): Boolean = {
      if (if (withComma) lookAhead(builder, ScalaTokenTypes.tCOMMA, ScalaTokenTypes.tIDENTIFIER, ScalaTokenTypes.tAT,
        ScalaTokenTypes.tUNDER, ScalaTokenTypes.tIDENTIFIER)
      else lookAhead(builder, ScalaTokenTypes.tIDENTIFIER, ScalaTokenTypes.tAT,
        ScalaTokenTypes.tUNDER, ScalaTokenTypes.tIDENTIFIER)) {
        if (withComma) builder.advanceLexer() // ,
        ParserUtils.parseVarIdWithWildcardBinding(builder, builder.mark())
      } else false
    }

    if (!parseSeqWildcard(false) && !parseSeqWildcardBinding(false) && Pattern.parse(builder)) {
      while (builder.getTokenType == ScalaTokenTypes.tCOMMA && !parseSeqWildcard(true) && !parseSeqWildcardBinding(true)) {
        builder.advanceLexer() // eat comma
        Pattern.parse(builder)
      }
    }
    args.done(ScalaElementTypes.PATTERNS)
    true
  }
}