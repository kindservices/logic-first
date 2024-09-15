# logic-first

## What?
This project provides tools for creating documentation (sequence diagrams, c4 architectural diagrams, etc) from working software.

Instead of starting with architecture diagrams or documentation from which we create software, this '#logic-first' approach starts with working software which can then generate the documentation.

## Why?
The typical process of documenting, design, prototype, build, and release software has a pretty major flaw:

```shell
Does the thing you built match what was designed?
```

Software is infinitely maliable and chances all the time. Unfortunately, the documentation and diagrams very rarely change with it.

What's more, it's very easy to draw an architecture which doesn't work. One which has logical holes in it, unwritten or unnoticed assumptions, or that just doesn't hang together.

If we start from the logic _first_ (i.e. working code which models the data which flows through the system) and use that to derive our documents, it realises a number of benefits:

 * Our diagrams 'compile' -- we spot issues much earlier
 * There is no documentation drift -- if you're looking at a diagram of your system, you know that's how it works
 * It tightens the feedback loop and connection between the software system and the data you use to drive it
 * You get diagrams which cover all the cases (not just happy-path), and which can compose (e.g. cover components, as well as the entire system)

## How?
'#logic-first' is a concept - it can be done in any language or technology. 
That says, some tools for the job are much better than others.

Having an expressive type system, mature tooling, and strong ecosystem are important.

Scala fits the bill for that, so this project combines the simplicity and effectiveness of Scala with the rich power of an IO type (ZIO) to demonstrate this approach.

It's as simple as writing interfaces and logic required, then instrumenting those operations to produce rich telemetry as you squirt data through those systems.



