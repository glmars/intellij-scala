package org.jetbrains.plugins.scala
package lang
package completion
package filters.toplevel

import com.intellij.psi.{PsiElement, _}
import com.intellij.psi.filters.ElementFilter
import org.jetbrains.annotations.NonNls
import org.jetbrains.plugins.scala.lang.completion.ScalaCompletionUtil._

/** 
* @author Alexander Podkhalyuzin
* Date: 22.05.2008
*/

class ImportFilter extends ElementFilter {
  def isAcceptable(element: Object, context: PsiElement): Boolean = {
    if (context.isInstanceOf[PsiComment]) return false
    val (leaf, _) = processPsiLeafForFilter(getLeafByOffset(context.getTextRange.getStartOffset, context))
    
    if (leaf != null) {
      val parent = leaf.getParent
      val tuple = ScalaCompletionUtil.getForAll(parent,leaf)
      if (tuple._1) return tuple._2
    }
    false
  }

  def isClassAcceptable(hintClass: java.lang.Class[_]): Boolean = true

  @NonNls
  override def toString = "'import' keyword filter"
}