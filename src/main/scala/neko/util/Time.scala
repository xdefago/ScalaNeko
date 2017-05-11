/*
 * Copyright 2017 Xavier Défago (Tokyo Institute of Technology)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package neko.util

import java.text.DecimalFormat

import scala.concurrent.duration.{ Duration, NANOSECONDS }

/**
 * type-safe representation of time in ScalaNeko.
 * The internal representation stores the number of nanoseconds since the start of the execution.
 * Since the value is stored in a `Long`, this allows to represent time for up to
 * 2^63^ns, i.e., nearly 300 years.
 *
 * @param value  internal representation of time in nanoseconds.
 */
case class Time(value: Long) extends Serializable with Ordered[Time]
{
  def + (that: Time) = map2(that)(_ + _)
  def - (that: Time) = map2(that)(_ - _)
  def / (that: Time) = combine(that)(_ / _)
  def / (divisor: Long)   = Time(value / divisor)
  def / (divisor: Double) = Time((value / divisor).toLong)
  def * (factor: Long)    = Time(value * factor)
  def * (factor: Double)  = Time((value * factor).toLong)

  def compare (that: Time): Int = combine(that)(_ compare _)

  def succ : Time = map (_+1)
  def pred : Time = map (_-1)

  def before (that: Time) = this < that
  def after  (that: Time) = this > that

  def min (that: Time): Time = if (this <= that) this else that
  def max (that: Time): Time = if (this >= that) this else that

  def combine[A] (that: Time)(f: (Long, Long) => A): A = f(value, that.value)
  def map2 (that: Time)(f: (Long, Long) => Long): Time = Time(combine(that)(f))
  def map (f: (Long)=> Long): Time = Time(f(value))

  /**
   * returns a string representation of this time expressed in nanoseconds.
   *
   * @return string representation
   */
  def asNanoseconds: String = s"${value}ns"

  /**
   * returns a string representation of this time expressed in seconds with precision to the microsecond.
   *
   * @return string representation
   */
  def asSeconds: String   = Time.formatTime(this, Time.second, Time.microsecond) + "s"

  /**
   * converts this time value to a [[scala.concurrent.duration.Duration]].
   *
   * @return a duration equivalent to this time value
   */
  def toDuration: Duration = Duration(value, NANOSECONDS)
}


object Time
{
  val ZERO = Time(0)

  val nanosecond  = Time(1)
  val microsecond = nanosecond * 1000
  val millisecond = microsecond * 1000
  val second      = millisecond * 1000

  val NANOSECOND  = nanosecond.value
  val MICROSECOND = microsecond.value
  val MILLISECOND = millisecond.value
  val SECOND      = second.value

  private val decimalFormat = new DecimalFormat("0.##################")

  def formatTime(time: Time, unit: Time, round: Time): String =
  {
    assume(unit >= round)
    val numDigits = math.round(math.log10(unit.value / round.value)).toInt
    val decimal   = (BigDecimal(time.value) / BigDecimal(unit.value)).setScale(numDigits, BigDecimal.RoundingMode.HALF_UP)
    if (numDigits <= 3) {
      decimal.toString()
    } else {
      decimalFormat.format(decimal)
    }
  }

  /**
   * formats time as a string expressed in seconds, with precision to the millisecond.
   *
   * @param time    time to format
   * @return        string expressed in seconds, with precision to the millisecond
   */
  def formatTimeSeconds(time: Time): String =
  {
    val seconds = time.value / SECOND
    val millis  = (time.value % SECOND) / MILLISECOND
    f"$seconds%3d.$millis%03d"
  }
}
