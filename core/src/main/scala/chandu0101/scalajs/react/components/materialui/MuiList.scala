package chandu0101.scalajs.react.components
package materialui

import chandu0101.macros.tojs.JSMacro
import japgolly.scalajs.react._
import scala.scalajs.js
import scala.scalajs.js.`|`

/**
 * This file is generated - submit issues instead of PR against it
 */
    
case class MuiList(
  key:            js.UndefOr[String]        = js.undefined,
  ref:            js.UndefOr[String]        = js.undefined,
  /* If true, the subheader will be indented by 72px.*/
  insetSubheader: js.UndefOr[Boolean]       = js.undefined,
  /* Override the inline-styles of the root element.*/
  style:          js.UndefOr[CssProperties] = js.undefined,
  /* The subheader string that will be displayed at the top of the list.*/
  subheader:      js.UndefOr[ReactNode]     = js.undefined,
  /* The style object to override subheader styles.*/
  subheaderStyle: js.UndefOr[CssProperties] = js.undefined,
  /* The zDepth prop passed to the Paper element inside list.*/
  zDepth:         js.UndefOr[ZDepth]        = js.undefined){
  /**
   * @param children These are usually ListItems that are passed to
be part of the list.
   */
  def apply(children: ReactNode*) = {
    val props = JSMacro[MuiList](this)
    val f = React.asInstanceOf[js.Dynamic].createFactory(Mui.List)
    if (children.isEmpty)
      f(props).asInstanceOf[ReactComponentU_]
    else if (children.size == 1)
      f(props, children.head).asInstanceOf[ReactComponentU_]
    else
      f(props, children.toJsArray).asInstanceOf[ReactComponentU_]
  }
}
