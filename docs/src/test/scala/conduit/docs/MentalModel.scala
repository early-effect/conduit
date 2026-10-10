package conduit.docs

import mermoid.Mermaid
import mermoid.ascent.MermoidAscent
import specular.*
import zio.test.*

object MentalModel extends DocSpec:

  private val runModes =
    Mermaid("""flowchart TB
      |  Enq["c(action)"] --> Q[Queue]
      |  Q --> Drain["run() drain to empty"]
      |  Q --> Live["run(false) until Done"]
      |  Drain --> Idle[Return]
      |  Live --> Wait[Block on queue]
      |  Wait --> Live
      |""".stripMargin)

  private val skip =
    Mermaid("""flowchart TB
      |  Act[Action] --> H[Handler]
      |  H --> Dirty{dirty and model FastEq?}
      |  Dirty -->|no| SkipAll[Skip every listener]
      |  Dirty -->|yes| Each[Each Listener]
      |  Each --> Slice{slice FastEq?}
      |  Slice -->|equal| Quiet[No callback]
      |  Slice -->|changed| Fire[listener effect]
      |""".stripMargin)

  private val follow =
    Mermaid("""flowchart LR
      |  A[Action] --> H[Handler]
      |  H --> M[New model]
      |  H --> N[next]
      |  N --> H2[Nested dispatch]
      |  H2 --> M
      |""".stripMargin)

  def doc = page("Mental model")(
    md"""
Three details that surprise people coming from Redux-in-JS: **enqueue is not dispatch**,
**FastEq can skip the whole listener set**, and **follow-ups run nested inside the current
dispatch**, not as a later tick.
""",
    section("run vs run(false)")(
      md"""
Tests and scripts use `run()` so the effect completes. Long-lived apps (and these live docs)
use `run(false)` and eventually `Done`. A completed `forkDaemon` can be reaped; keep the loop
on a scope (`forkScoped`) or on the main fiber.
""",
      example {
        MermoidAscent.diagram(runModes)
      }.assert(ui => assertTrue(ui.toString.contains("drain to empty"))),
      exampleIO {
        MermoidAscent.diagramInteractive(runModes, initialWidth = 640)
      }.interactive.assert(ui => assertTrue(ui.toString.contains("mermoid-ascent"))),
    ),
    section("Two FastEq gates")(
      md"""
After the handler, Conduit compares the **whole model**. If that says unchanged, no listener is
even asked. If the model changed, each listener compares **its slice** to the last value it saw.
A listener on `_.label` does not run when only `_.count` moved.
""",
      example {
        MermoidAscent.diagram(skip)
      }.assert(ui => assertTrue(ui.toString.contains("Skip every listener"))),
    ),
    section("Follow-ups are nested")(
      md"""
`ActionResult.next` is dispatched before `run()` looks at the queue again. `IncTwice` that
returns `Inc` as follow-up becomes two handler runs in one `c(IncTwice)` + `run()`.
""",
      example {
        MermoidAscent.diagram(follow)
      }.assert(ui => assertTrue(ui.toString.contains("Nested dispatch"))),
      exampleIO {
        MermoidAscent.diagramInteractive(follow, initialWidth = 640)
      }.interactive.assert(ui => assertTrue(ui.toString.contains("mermoid-ascent"))),
    ),
  )
end MentalModel
