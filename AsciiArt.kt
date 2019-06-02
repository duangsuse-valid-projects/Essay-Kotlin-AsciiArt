import java.awt.image.BufferedImage
import java.awt.Color

import javax.imageio.ImageIO

import java.net.URL
import java.io.*

typealias Coord2D = BufferedImage
typealias Axis2D = Int
typealias Point2D = Pair<Axis2D, Axis2D>

operator fun BufferedImage.get(xy: Point2D): Color {
  val (x, y) = xy
  return this.getRGB(x, y).let(::Color)
}

operator fun Point2D.plus(offset: Point2D): Point2D {
  val (ox, oy) = this
  val (xo, yo) = offset
  return Point2D(ox+xo, oy+yo)
}

object AsciiArtKt {
  private fun longPlus(x: Long, y: Number) = x +y
  private fun longDiv(x: Long, y: Number) = x /y
  private inline operator fun <reified T: Number> T.plus(other: T): T
    = longPlus(this.toLong(), other) as T
  private inline operator fun <reified T: Number> T.div(other: T): T
    = longDiv(this.toLong(), other) as T

  private inline fun <reified T: Number> Collection<T>.sum(): T
    = this.fold(0 as T) { ac, x -> ac +x }

  private inline fun <reified T: Number> Collection<T>.average(): T
    = this.sum() / this.size as T

  /**
   * Read 4 edge pixel, determind with average function
   */
  private fun bitmapColorBlkAverage(bmp: Coord2D, xy: Point2D, blksz: Axis2D): Color {
    val sampled = blksz -1

    val ld = bmp[xy]
    val rd = bmp[xy + Point2D(sampled, 0)]
    val lt = bmp[xy + Point2D(0, sampled)]
    val rt = bmp[xy + Point2D(sampled, sampled)]

    val allpxs = arrayOf(ld, rd, lt, rt)
    val reds = allpxs.map { it.red }
    val greens = allpxs.map { it.green }
    val blues = allpxs.map { it.blue }

    return Color(reds.average(), greens.average(), blues.average())
  }

  private fun imageOf(stm: InputStream?) = stm?.let(ImageIO::read)
  private fun imageOf(url: URL?) = url?.let(ImageIO::read)
  private fun imageOf(vararg argv: String)
    = argv.firstOrNull().let {
      if (it == "url") argv.getOrNull(1)?.let(::URL).let(::imageOf)
      else it?.let(::File)
      ?.let(::FileInputStream)
      ?.let(::BufferedInputStream)
      ?.run { imageOf(this) }
    }

  private const val BLACK = 'M'
  private const val WHITE = '_'

  private const val BOUNDARY = 0x7f

  fun boundaryMapper(bound: Int = BOUNDARY) = fun (ch: Char, ch1: Char) = fun (cp: Color): Char {
    val sum = cp.red + cp.green + cp.blue
    return if (sum >= bound) ch else ch1
  }

  fun Color.similarChar(mapper: (Color) -> Char): Char = mapper(this)

  @JvmStatic fun main(vararg args: String) {
    val img = imageOf(*args)
    println(img)
    val skp = System.getProperty("skip", "2").toInt()
    val cmapper = boundaryMapper()(BLACK, WHITE)

    if (img ==null) return

    for (row in 0 until img.height step skp) {
    for (col in 0 until img.width step skp) {
      val colorAvg = bitmapColorBlkAverage(img, Point2D(col, row), skp)
      print(colorAvg.similarChar(cmapper))
    }; println() }
  }
}
