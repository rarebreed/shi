Importing
=========

Importing libs in clojure is confusing.  In order to really understand them, you have to
understand vars, bindings, and namespaces.

var- A symbol that may represent something else
binding- maps a var to some value
namespace- maps bindings (not vars) to a name (for easy look up)

require
-------

The require directive will import a *clojure* library into the current namespace.  In order to use
the new var, it must be fully qualified::

    (require 'clojure.string)
    (require '[clojure.java])

:as allows one to alias the namespace to another name

What is confusing is that when you use require in the REPL, you must quote the namespace.  However,
if you are using the *ns* function you don't quote it, you use the keyword :require, and if you use
any directive::

    (ns shi.commander
      (:require clojure.string)
      (:require [clojure.reflect :as r]))


Comparison to python
--------------------

Python::

    # somefile.py
    import logging                      # 1. general import
    from subprocess import Popen, PIPE  # 2. import what you need
    import queue as q                   # 3. alias a module
    import . mymodule                   # 4. relative import
    from . mypackage.test import Test   # 5. relative import from a package a classes


Clojure::

    (ns mystuff
      (:require 'clojure.tools.logging)
      (:require []))



Introspection
=============

One of the great things about python was how easy it was to introspect packages, modules, classes, etc
and I really wanted this feature too.  When learning a new language or experiment with a new feature
or idea, working at the REPL can't be beat.

However, it's not abundantly clear how to do introspection in clojure.  Here are some notes I have
taken in regards to this.

Docstrings
----------

Like python, clojure has docstrings.  In order to read the docstring at the REPL, you can use the
*doc* function like this::

    (defn my-func [x & y]
      "destructuring example that takes a sequence, extracts the first element into x, and puts
       the remaining items in y"
      (do
        (println x)
        (println y)))

    (doc my-func)

There is also the *find-doc* function if you aren't sure of the name of the function.  It takes a
string or regular expression and returns all matches

Namespaces
----------

Since namespaces act as containers for symbols, it's often a good idea to know what's in them.  The
most common ones you will probably use are *ns-interns*, *ns-publics* and *ns-imports*.  If you want
the whole enchilada, you might use *ns-map*.

Examples::

  (require 'clojure.string)
  (doseq [ x (ns-map 'clojure.string) ]
    (println x))


Objects
-------

Sometimes you have some object and you want to see what it is capable of.  Normally this is used
during java interop, but it might be useful on multimethods, protocols, deftypes and defrecords

The best way to do this is to use the clojure.reflect library::

    (require '[clojure.reflect :as r])
    (use '[clojure.pprint :only [print-table]])

    (def pb (ProcessBuilder. ["ls -al"]))

    (print-table (sort-by


destructuring
=============

They say that lisps don't have much syntax to learn, but one area where this is not true is in the
mini-destructuring language built into clojure.  This provides syntactic sugar to make it easier
to pull out elements from a data structure.  Some examples.

A function that takes a map, and gets keys called user and password::

    (defn get-token [{:keys [user password url]}] ; this differs slightly from the let syntax
      (send-request url user password))  ; we have extracted the map passed into get-token by keywords

    (def auth {:user "sean" :pass "idunno" :url "http://foo/secret"})
    (get-token auth)  ; notice, we only pass in the map, auth

    ; The let syntax is a bit different
    (let [{:keys [user password url]} auth]  ; the let binding has the map var as the 2nd element
      (println user password url))

Records, Types, and multimethods
================================

One quirk about creating a defrecord is the funny -> that gets prepended to the defrecord type.
For example, if you have a defrecord like this::

    ; cool_project.clj
    (ns cool-project
      (:require [clojure.pprint :as cpp]))

    (defrecord Person [name age company])

    ; in the REPL
    (require 'cool-project)
    (def me (cool-project/->Person "Sean Toner" 42 "RedHat"))


Pre and post conditions
=======================

You must put this as the first thing after the arguments.  IE, this must go before the docstring


Metadata on defn
================

Although the :pre and :post map in a defn looks like metadata, it isn't.  Remember, metadata is represented
as::

    (defn some-test [x]
      ^{:skipped ["single-node"]}
      ...
      )
