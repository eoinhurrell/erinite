# Erinite

> A hydrous arseniate of copper, of an emerald-green color; -- so called from
> Erin, or Ireland, where it occurs

Erinite is a web framework for interactive web applications. Erinite allows the
development of both server and client in Clojure(Script) as highly decoupled,
reactive, composable components.

## Rationale

There are already a number of great web frameworks available and some very
promising ones currently in development. Most follow the Rails/Django model, or
the Flask/Sinatra model, or the Lift model. While they work well for what they
were designed to do, the reactive models used by Hoplon and Pedestal appear to
follow the Clojure philosophies of decomplection and simplicity much more
closely. Erinite therefore is modelled after these concepts and looks quite
similar to Pedestal. Where Erinite hopes to set itself apart from Pedestal is
through a composable mix-and-match component-based architecture and heavier
emphasis on functional-reactive messages.

The final reason for Erinites existence is that, at the time of writing,
Pedestal was not yet close to production ready (and client-side Pedestal in
limbo), while we wanted something we can use right now.

## Design Goals

* Loose coupling between components
* Easy independent testing of components
* Simulate components; support recording and playback of execution
* Designer friendly development of UI templates
* Reactive user interaction logic
* Empower interactive development

The first three points are achieved through loosely coupled services that
communicate through messages.
The fourth point is achieved through logic-less HTML templates using enlive.
The fifth point is achieved by communicating between input, logic and output
through a functional-reactive core.

The final point is achieved through the following:
* Data-centric design
* Reloadable, restartable compontents
* Ease of hooking in interactive tools
* REPL friendly API

These goals were chosen so that applications written using Erinite are easy to
develop, easy to test, easy to think about, easy to modify and easy to extend,
even when implementing complex logic or operating at scale. It is dificult to
know future requirements, so enabling quick, safe (breaking changes are caught
early) and convenient modification is the only way to stay ahead of the
competition.

A secondary goal of Erinite is to make use of existing libraries and tools
where possible, to avoid duplication of effort and to allow libraries to be
replaced or removed as appropriate. A core philosophy of Clojure is to write
small composable libraries and Erinite should embrace this philosophy whenever
possible.

## Design Overview

Erinite provides a layered architecture, splitting an application into Logic and
Services. Services are components that talk to the outside world (client/server,
third party services, databases, user interfaces) while Logic are functions that
transform the application state.

Both the server and client follow an identical architecture (and most of the
implementation is shared code).

## License

Copyright © 2014 Actionable Insight Software Ltd.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
