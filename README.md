# Erinite

> A hydrous arseniate of copper, of an emerald-green color; -- so called from
> Erin, or Ireland, where it occurs

Erinite is a web framework for interactive web applications. Erinite allows the
development of both server and client in Clojure(Script) as highly decoupled,
reactive, composable components.

**WARNING: Erinite is pre-alpha and should be considered experimental**

**WARNING 2: Erinite is still very eary and does not yet meet the design goals**

## Rationale


## Design Goals

* Loose coupling between components
* Easy independent testing of components
* Simulate components; support recording and playback of execution
* Designer friendly development of UI templates
* Reactive user interaction logic
* Empower interactive development

The following secondary goals exist to enable to above:
* Data-centric design
* Reloadable, restartable compontents
* Communication through functional-reactive messaging
* Ease of hooking in interactive tools
* REPL friendly API

An auxiliary goal of Erinite is to make use of existing libraries and tools
where possible, to avoid duplication of effort and to allow libraries to be
replaced or removed as appropriate. A core philosophy of Clojure is to write
small composable libraries and Erinite should embrace this philosophy whenever
possible.

These goals were chosen so that applications written using Erinite are easy to
develop, easy to test, easy to think about, easy to modify and easy to extend,
even when implementing complex logic or operating at scale. It is dificult to
know future requirements, so enabling quick, safe (breaking changes are caught
early) and convenient modification is the only way to stay ahead of the
competition.


## Design Overview


## License

Copyright © 2014 Actionable Insight Software Ltd.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
