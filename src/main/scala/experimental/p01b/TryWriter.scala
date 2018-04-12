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

package experimental.p01b

import java.io.PrintStream


/**
 * Created by defago on 26/04/2017.
 */
object TryWriter extends App
{
  import neko.io._
  
  val out = new PrintStream(new PrefixOutputStream("XYZ: ", Console.out))
  
  out.println("Hello World!")
  out.println(s"This is\na multiline\nstring that ends with\na line separator\n")
  out.print(s"This is\na second multiline\nstring")
  out.println(" that was printed.")
}
