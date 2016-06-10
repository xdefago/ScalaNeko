# ScalaNeko

This is a development platform aimed at prototyping distributed algorithms
and as a support for teaching such algorithms. The platform is written in
Scala and allows to write distributed algorithms in a very concise yet
easily readable way.

ScalaNeko is based on the Neko toolkit; a project originally developed
at EPFL by Péter Urbán, Xavier Défago, and several other contributors.
The Neko toolkit was written in Java and allowed, *without changing its
code*, to execute a distributed program either on a real network or in a
simulation. The purpose was to support the lifecycle of a distributed
algorithm from design to performance evaluation, under the premise that
a change in writing style between simulation and execution would be an
undesirable factor affecting performance measurements.

While ScalaNeko does not retain the ability to execute on a real network
(i.e., it supports simulation-only), its purpose is exclusively aimed at
teaching. With this in mind, great efforts where directed at simplifying
the way distributed algorithms would be expressed in that model.
ScalaNeko was successfully used as programming support for lectures on
distributed systems at JAIST, Hiroshima University.

Ocelot, a successor to ScalaNeko, will be used in future lectures at
Tokyo Institute of Technology.
