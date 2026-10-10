package conduit.docs

import _root_.conduit.{ActionHandler, Conduit, FastEq}
import mermoid.{Mermaid, RenderConfig, ResponsiveConfig, Viewport}
import mermoid.ascent.MermoidAscent
import specular.*
import zio.test.*

object Overview extends DocSpec:

  private val loop =
    Mermaid("""flowchart LR
      |  Act[Action] --> Q[Queue]
      |  Q --> Disp[Dispatch]
      |  Disp --> H[Handler]
      |  H --> R[Ref]
      |  R --> Eq{FastEq}
      |  Eq -->|changed| L[Listeners]
      |  Eq -->|same| Skip[Skip notify]
      |  H --> Next[Follow-ups]
      |  Next --> Disp
      |""".stripMargin)

  /** Below 900px the loop stacks. 420 fits a 390px column and stays full size on a desktop. */
  private val loopConfig =
    RenderConfig(responsive = ResponsiveConfig(flipDirectionBelow = Some(900)))

  def doc = page("Overview")(
    md"""
Actions go in, a handler returns the next model, and a listener runs only when FastEq says its slice changed.
""",
    section("The loop")(
      md"""
Enqueue is cheap and asynchronous. **Nothing is applied until `run()`**. Follow-ups returned from a
handler are dispatched immediately (nested), then the loop continues. Click a node to highlight it.
""",
      example {
        MermoidAscent.diagram(loop, config = loopConfig, viewport = Some(Viewport(420)))
      }.assert(ui => assertTrue(ui.toString.contains("FastEq"), ui.toString.contains("Listeners"))),
      exampleIO {
        MermoidAscent.diagramInteractive(loop, config = loopConfig, initialWidth = 420)
      }.interactive.assert(ui => assertTrue(ui.toString.contains("mermoid-ascent"), ui.toString.contains("Narrow"))),
    ),
    section("A running counter")(
      md"""
The number below is a real `Conduit`, the same widget as [Getting started](getting-started.html).
`+` enqueues `Inc`. The loop applies the handler, and the listener patches the text only because the
count changed.
""",
      cite(
        Conduit.make[Int, Nothing](_: Int)(_: ActionHandler[Int, ?, Nothing])(using _: FastEq[Int])
      ),
      exampleIO {
        GettingStarted.liveCounter
      }.interactive.assert(ui => assertTrue(ui.toString.contains("Reset"), ui.toString.contains("+"))),
    ),
    section("Install")(
      md"""
```scala
libraryDependencies += "rocks.earlyeffect" %% "conduit" % "<version>"
```

Use `%%%` on Scala.js or Native. These live widgets use
[ascent-conduit](https://www.earlyeffect.rocks/ascent/). That bridge is optional and is not a
dependency of the published artifact.

| Piece | Job |
| --- | --- |
| **Model** | Immutable case class, usually `derives Optics` |
| **Action** | `enum X extends Action` (alias for `AppAction[Any, Nothing]`) |
| **Handler** | Partial function from action to `ActionResult` on a lensed slice |
| **Conduit** | Queue + `Ref` + dispatch loop + listeners |

Read [Getting started](getting-started.html) for the handler, then [Mental model](mental-model.html)
for `run` vs `run(false)`.
"""
    ),
  )
end Overview
