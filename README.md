# ScalaNeko

ScalaNeko is a development platform aimed at prototyping distributed algorithms
and as a support for teaching the design of such algorithms. The platform is written in
Scala and allows to write distributed algorithms in a very concise yet
easily-readable way.

ScalaNeko is based on the Neko toolkit [[1,2]](#Refs); a project originally developed
at EPFL by Péter Urbán, Xavier Défago, and several other contributors in
the laboratory of Prof. André Schiper (now retired).
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
distributed systems at [JAIST](www.jaist.ac.jp) and at Hiroshima University.

This version of ScalaNeko is useable but rough around the edges.
The documentation generated through scaladoc provide some information on how to use it and its
syntax, but it is partly stale. Although the repository is made public, we do not consider it
to be easily useable without explicit instructions.
Anyone is free to install and use ScalaNeko in its current form, but this is at your own risk and
no support will be offered about it.
In contrast, we are currently developing Ocelot, a successor to ScalaNeko. It will be used in
future lectures at [Tokyo Institute of Technology](www.titech.ac.jp) and we intend to provide a
complete documentation and full support for it.

### <a name="Refs"></a> References

1. P. Urbán, X. Défago, and A. Schiper.
   [Neko: A single environment to simulate and prototype distributed algorithms](http://www.iis.sinica.edu.tw/JISE/2002/200211_07.html).
   _Journal of Information Science and Engineering_, 18(6):981-997, November 2002.

2. P. Urbán, X. Défago, and A. Schiper.
   [Neko: A single environment to simulate and prototype distributed algorithms](http://dx.doi.org/10.1109/ICOIN.2001.905471).
   In _Proc. 15th IEEE Intl. Conf. on Information Networking (ICOIN)_, pp. 503-511, Beppu City, Japan, January 2001.
