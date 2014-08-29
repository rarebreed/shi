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
    (require '[clojure.java 

:as allows one to alias the namespace to another name

What is confusing is that when you use require in the REPL, you must quote the namespace.  However,
if you are using the *ns* function you don't quote it, you use the keyword :require, and if you use
any directive::

    (ns shi.commander
      (:require clojure.string)
      (:require [clojure.reflect :as r]))


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


