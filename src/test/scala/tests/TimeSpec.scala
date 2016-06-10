/**
 *
 * Copyright 2015 Xavier Defago
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Created by IntelliJ IDEA.
 * User: defago
 * Date: 08/06/15
 * Time: 09:36
 *
 */
package tests

import neko.util.Time
import org.scalatest.FlatSpec

class TimeSpec extends FlatSpec
{
  behavior of "Time"

  it should "create custom units time strings" in {
    val times = List(
      (Time(123456789), "0.123456789", "123.456789", "123456.789"),
      (Time(7), "0.000000007", "0.000007", "0.007"),
      (Time(1000), "0.000001", "0.001", "1")
    )

    for ((time, asSec, asMilli, asMicro) <- times) {
      assertResult(asSec)(Time.formatTime(time, Time.second, Time.nanosecond))
    }
  }
}
