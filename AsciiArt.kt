import java.awt.image.BufferedImage
import java.awt.Color

import javax.imageio.ImageIO

import java.net.URL
import java.io.*

typealias Coord2D = BufferedImage
typealias Axis2D = Int
typealias Point2D = Pair<Axis2D, Axis2D>

/**
 * Get `Color` pixel from image coordinate
 */
operator fun BufferedImage.get(xy: Point2D): Color {
  val (x, y) = xy
  return this.getRGB(x, y).let(::Color)
}

/**
 * Plus a point (Pair of `Axis2D`) with another
 */
operator fun Point2D.plus(offset: Point2D): Point2D {
  val (ox, oy) = this
  val (xo, yo) = offset
  return Point2D(ox+xo, oy+yo)
}

internal fun <T: Number> longPlus(x: T, y: T): Long = x.toLong() + y.toLong()
internal fun <T: Number> longDiv (x: T, y: T): Long = x.toLong() + y.toLong()

/** Empty type set */
@Suppress("unused") typealias `∅` = Nothing

@Throws(RuntimeException::class)
fun impossible(): Nothing = throw RuntimeException("Impossible program state reached!")

inline fun <reified T> classOf(): Class<T> = T::class.java

fun <T: Number, Y: Number, C: Class<Y>> numericCoerce(x: T): (C) -> Y {
  val (byteK, shortK) = Pair(classOf<java.lang.Byte>(), classOf<java.lang.Short>())
  val (intK, longK) = Pair(classOf<java.lang.Integer>(), classOf<java.lang.Long>())
  val (floatK, doubleK) = Pair(classOf<java.lang.Float>(), classOf<java.lang.Double>())

  // well done, kotlin compiler.
  // what her made me do...
  // is? is? is Class???

  // （汗）弄了半天其实误会了，原来 byteK 这些是对象不需要 is byteK 判断...
  return fun (k: C): Y {
    // well done, kotlin compiler.
    // SEE what her made me do...
    // SEE what her made me do...

    when (k) {
      byteK  -> x::toByte
      shortK -> x::toShort
      intK   -> x::toInt
      longK  -> x::toLong
      floatK  -> x::toFloat
      doubleK -> x::toDouble
      else -> impossible()
    }.invoke().let { return it as Y }
  }
}

object AsciiArt {
  private inline operator fun <reified T: Number> T.plus(other: T): T
    = longPlus(this, numericCoerce<T, java.lang.Long, Class<java.lang.Long>>(other)(java.lang.Long::class.java)).run {
      numericCoerce<T, java.lang.Long, Class<java.lang.Long>>(this)(java.lang.Long::class.java) as T
    }
  private inline operator fun <reified T: Number> T.div(other: T): T
    = longDiv(this, numericCoerce<T, java.lang.Long, Class<java.lang.Long>>(other)(java.lang.Long::class.java)).run {
      numericCoerce<T, java.lang.Long, Class<java.lang.Long>>(this)(java.lang.Long::class.java) as T
    }

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

  /**
   * Receives boundary, 2 colors (for graterEquals case, lower case), return a `Color` -> `Char` mapper function
   */
  fun boundaryMapper(bound: Int = BOUNDARY) = fun (ch: Char, ch1: Char)
  = fun (cp: Color): Char {
    val sum = cp.red + cp.green + cp.blue
    return if (sum >= bound) ch else ch1
  }

  val skp: Int by lazy(LazyThreadSafetyMode.PUBLICATION) { System.getProperty("skip", "2").toInt() }

  fun Color.similarChar(mapper: (Color) -> Char): Char = mapper(this)

  @JvmStatic fun main(vararg args: String) {
    val img = imageOf(*args).also { println(it ?: "Please specify one image path (or \"url\" scheme:)") }

    val cmapper = boundaryMapper()(BLACK, WHITE)

    if (img ==null) return

    for (row in 0 until img.height step skp) {
    for (col in 0 until img.width step skp) {
      val colorAvg = bitmapColorBlkAverage(img, Point2D(col, row), skp)
      print(colorAvg.similarChar(cmapper))
    }; println() }

    println()
  }
}
