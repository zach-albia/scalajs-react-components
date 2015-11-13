package chandu0101.scalajs.react.components
package elementalui

import japgolly.scalajs.react._
import scala.scalajs.js

case class Form(
    key: U[String] = uNone,
    className: U[String] = uNone,
    style: U[String] = uNone,
    `type`: U[FormType] = uNone) {

  def apply(children: ReactNode*) = {
    val props = JSMacro[Form](this)
    val f = React.asInstanceOf[js.Dynamic].createFactory(Eui.Form)
    f(props, children.toJsArray).asInstanceOf[ReactComponentU_]
  }

}

case class FormType private (val value: String) extends AnyVal

object FormType {
  val BASIC = FormType("basic")
  val HORIZONTAL = FormType("horizontal")
  val INLINE = FormType("inline")
}
