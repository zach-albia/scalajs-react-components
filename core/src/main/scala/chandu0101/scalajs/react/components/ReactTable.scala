package chandu0101.scalajs.react.components

import japgolly.scalajs.react.vdom.html_<^._

import japgolly.scalajs.react.Callback
import japgolly.scalajs.react.BackendScope
import japgolly.scalajs.react.ScalaComponent
import scala.collection.immutable
import scalacss.ProdDefaults._

import scalacss.ScalaCssReact.scalacssStyleaToTagMod
import chandu0101.scalajs.react.components.ReactSearchBox
import chandu0101.scalajs.react.components.Pager
import chandu0101.scalajs.react.components.DefaultSelect

/**
  * Companion object of ReactTable, with tons of little utilities
  */
object ReactTable {

  /**
    * The direction of the sort
    */
  object SortDirection extends Enumeration {
    type SortDirection = Value
    val asc, dsc = Value
  }
  /*
   * Pass this to the ColumnConfig to sort using an ordering
   */
  def Sort[T, B](fn: T => B)(implicit ordering: Ordering[B]): (T, T) => Boolean = {
    (m1: T, m2: T) =>
      ordering.compare(fn(m1), fn(m2)) > 0
  }
  /*
   * Pass this to the ColumnConfig to sort a string ignoring case using an ordering
   */
  def IgnoreCaseStringSort[T](fn: T => String): (T, T) => Boolean =
    (m1: T, m2: T) ⇒ fn(m1).compareToIgnoreCase(fn(m2)) > 0

  class Style extends StyleSheet.Inline {

    import dsl._

    val reactTableContainer = style(display.flex, flexDirection.column)

    val table = style(
      display.flex,
      flexDirection.column,
      boxShadow := "0 1px 3px 0 rgba(0, 0, 0, 0.12), 0 1px 2px 0 rgba(0, 0, 0, 0.24)",
      media.maxWidth(740 px)(boxShadow := "none")
    )

    val tableRow = style(padding :=! "0.8rem",
                         &.hover(backgroundColor :=! "rgba(244, 244, 244, 0.77)"),
                         media.maxWidth(740 px)(boxShadow := "0 1px 3px grey", margin(5 px)))

    val tableHeader = style(fontWeight.bold, borderBottom :=! "1px solid #e0e0e0", tableRow)

    val settingsBar = style(display.flex, margin :=! "15px 0", justifyContent.spaceBetween)

    val sortIcon = styleF.bool(
      ascending ⇒
        styleS(
          &.after(fontSize(9 px), marginLeft(5 px), if (ascending) { content := "'\\25B2'" } else {
            content := "'\\25BC'"
          })))

  }

  object DefaultStyle extends Style

  type CellRenderer[T] = T ⇒ VdomNode

  def DefaultCellRenderer[T]: CellRenderer[T] = { model =>
    <.span(model.toString)
  }
  def EmailRenderer[T](fn: T => String): CellRenderer[T] = { t =>
    val str = fn(t)
    <.a(^.whiteSpace.nowrap, ^.href := s"mailto:${str}", str)
  }
  def OptionRenderer[T, B](defaultValue: String = "")(fn: T => Option[B]): CellRenderer[T] =
    t => fn(t).fold(defaultValue)(_.toString)

  case class ColumnConfig[T](name: String,
                             cellRenderer: CellRenderer[T],
                             sortBy: Option[(T, T) ⇒ Boolean] = None,
                             width: Option[String] = None,
                             nowrap: Boolean = false)

  def SimpleStringConfig[T](name: String,
                            stringRetriever: T => String,
                            width: Option[String] = None,
                            nowrap: Boolean = false): ReactTable.ColumnConfig[T] = {
    val renderer: CellRenderer[T] = if (nowrap) { t =>
      <.span(stringRetriever(t))
    } else { t =>
      stringRetriever(t)
    }
    ColumnConfig(name, renderer, Some(IgnoreCaseStringSort[T](stringRetriever)), width, nowrap)
  }
}

/**
  * A relatively simple html/react table with a pager.
  * You should pass in the data as a sequence of items of type T
  * But you should also pass a list of Column Configurations, each of which describes how to get to each column for a given item in the data, how to display it, how to sort it, etc.
  */
case class ReactTable[T](data: Seq[T],
                         configs: List[ReactTable.ColumnConfig[T]] = List(),
                         rowsPerPage: Int = 5,
                         style: ReactTable.Style = ReactTable.DefaultStyle,
                         enableSearch: Boolean = true,
                         searchBoxStyle: ReactSearchBox.Style = ReactSearchBox.DefaultStyle,
                         onRowClick: (Int) ⇒ Callback = { _ ⇒
                           Callback {}
                         },
                         searchStringRetriever: T => String = { t: T =>
                           t.toString
                         }) {

  import ReactTable._
  import SortDirection._

  case class State(filterText: String,
                   offset: Int,
                   rowsPerPage: Int,
                   filteredData: Seq[T],
                   sortedState: Map[Int, SortDirection])

  class Backend(t: BackendScope[Props, State]) {

    def onTextChange(P: Props)(value: String): Callback =
      t.modState(_.copy(filteredData = getFilteredData(value, P.data), offset = 0))

    def onPreviousClick: Callback =
      t.modState(s ⇒ s.copy(offset = s.offset - s.rowsPerPage))

    def onNextClick: Callback =
      t.modState(s ⇒ s.copy(offset = s.offset + s.rowsPerPage))

    def getFilteredData(text: String, data: Seq[T]): Seq[T] = {
      if (text.isEmpty) {
        data
      } else {
        data.filter(searchStringRetriever(_).toLowerCase.contains(text.toLowerCase))
      }
    }

    def sort(f: (T, T) ⇒ Boolean, columnIndex: Int): Callback =
      t.modState { S ⇒
        val rows = S.filteredData
        S.sortedState.get(columnIndex) match {
          case Some(asc) ⇒
            S.copy(filteredData = rows.sortWith((t1, t2) => !f(t1, t2)),
                   sortedState = Map(columnIndex -> dsc),
                   offset = 0)
          case _ ⇒
            S.copy(filteredData = rows.sortWith(f),
                   sortedState = Map(columnIndex -> asc),
                   offset = 0)
        }
      }

    def onPageSizeChange(value: String): Callback =
      t.modState(_.copy(rowsPerPage = value.toInt))

    def render(P: Props, S: State): VdomElement =
      <.div(
        P.style.reactTableContainer,
        ReactSearchBox(onTextChange = onTextChange(P) _, style = P.searchBoxStyle)
          .when(P.enableSearch),
        settingsBar((P, this, S)),
        tableC((P, S, this)),
        Pager(S.rowsPerPage, S.filteredData.length, S.offset, onNextClick, onPreviousClick)
      )
  }

  def getHeaderDiv(config: ColumnConfig[T]): TagMod = {
    config.width.fold(<.th())(width => <.th(^.width := width))
  }

  def arrowUp: TagMod =
    TagMod(^.width := 0.px,
           ^.height := 0.px,
           ^.borderLeft := "5px solid transparent",
           ^.borderRight := "5px solid transparent",
           ^.borderBottom := "5px solid black")

  def arrowDown: TagMod =
    TagMod(^.width := 0.px,
           ^.height := 0.px,
           ^.borderLeft := "5px solid transparent",
           ^.borderRight := "5px solid transparent",
           ^.borderTop := "5px solid black")

  def emptyClass: TagMod =
    TagMod(^.padding := "1px")

  val tableC = ScalaComponent
    .builder[(Props, State, Backend)]("table")
    .render { $ ⇒
      val (props, state, b) = $.props

      def renderHeader: TagMod =
        <.tr(
          props.style.tableHeader,
          props.configs.zipWithIndex.map {
            case (config, columnIndex) ⇒
              val cell = getHeaderDiv(config)
              config.sortBy.fold(cell(config.name.capitalize))(sortByFn =>
                cell(
                  ^.cursor := "pointer",
                  ^.onClick --> b.sort(sortByFn, columnIndex),
                  config.name.capitalize,
                  props.style
                    .sortIcon(state.sortedState.isDefinedAt(columnIndex) && state.sortedState(
                      columnIndex) == asc)
                    .when(state.sortedState.isDefinedAt(columnIndex))
              ))
          }.toTagMod
        )

      def renderRow(model: T): TagMod =
        <.tr(
          props.style.tableRow,
          props.configs
            .map(
              config =>
                <.td(^.whiteSpace.nowrap.when(config.nowrap),
                     ^.verticalAlign.middle,
                     config.cellRenderer(model)))
            .toTagMod
        )

      val rows = state.filteredData
        .slice(state.offset, state.offset + state.rowsPerPage)
        .zipWithIndex
        .map {
          case (row, i) ⇒ renderRow(row) //tableRow.withKey(i)((row, props))
        }
        .toTagMod

      <.div(props.style.table, <.table(<.thead(renderHeader()), <.tbody(rows)))
    }
    .build

  val settingsBar =
    ScalaComponent
      .builder[(Props, Backend, State)]("settingbar")
      .render { $ ⇒
        val (p, b, s)             = $.props
        var value                 = ""
        var options: List[String] = Nil
        val total                 = s.filteredData.length
        if (total > p.rowsPerPage) {
          value = s.rowsPerPage.toString
          options = immutable.Range
            .inclusive(p.rowsPerPage, total, 10 * (total / 100 + 1))
            .:+(total)
            .toList
            .map(_.toString)
        }
        <.div(p.style.settingsBar)(<.div(<.strong("Total: " + s.filteredData.size)),
                                   DefaultSelect(label = "Page Size: ",
                                                 options = options,
                                                 value = value,
                                                 onChange = b.onPageSizeChange))
      }
      .build

  val component = ScalaComponent
    .builder[Props]("ReactTable")
    .initialStateFromProps(p ⇒ State(filterText = "", offset = 0, p.rowsPerPage, p.data, Map()))
    .renderBackend[Backend]
    .componentWillReceiveProps(e =>
      Callback.when(e.currentProps.data != e.nextProps.data)(
        e.backend.onTextChange(e.nextProps)(e.state.filterText)))
    .build

  case class Props(data: Seq[T],
                   configs: List[ColumnConfig[T]],
                   rowsPerPage: Int,
                   style: Style,
                   enableSearch: Boolean,
                   searchBoxStyle: ReactSearchBox.Style)

  def apply() = component(Props(data, configs, rowsPerPage, style, enableSearch, searchBoxStyle))
}
