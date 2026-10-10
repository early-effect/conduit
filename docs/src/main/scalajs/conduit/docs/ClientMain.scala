package conduit.docs

import specular.client.SpecularClient
import zio.*

/** Browser entry: remount every `.interactive` example on the current page.
  *
  * One `ZIO.scoped` is the page lifetime, so a `Conduit` forked inside an example stays alive.
  * `InteractiveContractSpec` checks these pages against the site map.
  */
object ClientMain extends ZIOAppDefault:

  def run = ZIO.scoped {
    SpecularClient.mountAll(SpecularClient.fromPages(DocPages.all*)) *> ZIO.never
  }
end ClientMain
