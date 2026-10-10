package conduit.docs

import specular.*
import zio.test.*

/** The JS client remounts every key the site declares. `ClientMain` passes `DocPages.all`. */
object InteractiveContractSpec extends ZIOSpecDefault:

  def spec = suite("Interactive contract")(
    test("the site's mount keys are the pages the client remounts") {
      val keys = DocMounts.keys(DocPages.all*)
      assertTrue(
        keys.nonEmpty,
        keys == DocMounts.keys(BuildSite.pages*),
        DocMounts.domKeys(DocPages.all*).isEmpty,
      )
    }
  )
end InteractiveContractSpec
