package conduit.docs

import earlyeffect.docs.EarlyEffectTheme
import specular.site.*
import zio.*

import java.nio.file.{Files, Path, Paths}
import scala.jdk.OptionConverters.*

/** Docs-as-tests site builder (Test classpath; `docs/specularSite`). */
object BuildSite extends DocsSite:

  def pages = DocPages.all

  override def site(settings: DocsSettings): SiteModel =
    val m       = settings.meta
    val branded = EarlyEffectTheme.brand(super.site(settings))
    branded.copy(
      clientScript = Some("assets/client.js"),
      summaryMarkdown = Some(
        """Actions go in, a handler returns the next model, and a listener runs only when FastEq says its slice changed.

The dispatch loop and a running counter are on [Overview](overview.html).
"""
      ),
      installSnippets = Vector(
        ArtifactKind.defaultInstall(m, ArtifactKind.Library),
        CodeSnippet(
          "Scala.js / Native",
          s"""libraryDependencies += "${m.organization}" %%% "${m.name}" % "${m.docsVersion}"""",
        ),
      ),
      brand = Some(
        Brand(
          name = m.displayTitle,
          links = Vector(EarlyEffectTheme.github("https://github.com/early-effect/conduit")),
        )
      ),
    )
  end site

  override def layers: ZLayer[Any, Nothing, SiteBuilder] =
    EarlyEffectTheme.layers

  override def afterBuild(out: Path, result: SiteOutput): IO[SiteError, Unit] =
    val _ = result
    EarlyEffectTheme.writeLogo(out) *> copyClientBundle(out)

  private def copyClientBundle(out: Path): IO[SiteError, Unit] =
    ZIO
      .attemptBlocking(findClientJs)
      .orElseSucceed(None)
      .flatMap {
        case Some(src) => SiteAssets.copyFile(src, out.resolve("assets/client.js"))
        case None      => ZIO.fail(SiteError.MissingFile(clientJsMarker))
      }

  private def clientJsMarker: Path =
    repoRoot.resolve("target/specular-client-js.path")

  private def findClientJs: Option[Path] =
    readMarker.orElse(walkTargetOut)

  private def readMarker: Option[Path] =
    val marker = clientJsMarker
    if !Files.isRegularFile(marker) then None
    else
      val line = Files.readString(marker).trim
      if line.isEmpty then None
      else
        val path = Paths.get(line)
        Option.when(Files.isRegularFile(path))(path)
  end readMarker

  private def walkTargetOut: Option[Path] =
    val outRoot = repoRoot.resolve("target/out")
    if !Files.isDirectory(outRoot) then None
    else
      val stream = Files.walk(outRoot)
      try
        val found = stream
          .filter { p =>
            val s = p.toString.replace('\\', '/')
            s.endsWith("conduit-docs-fastopt/main.js")
          }
          .findFirst()
        found.toScala
      finally stream.close()
    end if
  end walkTargetOut

  private def repoRoot: Path =
    Iterator
      .unfold(Option(Paths.get("").toAbsolutePath))(_.map(p => (p, Option(p.getParent))))
      .find(p => Files.exists(p.resolve("build.sbt")))
      .getOrElse(Paths.get("").toAbsolutePath)
end BuildSite
