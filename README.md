# shi

A Clojure library designed to help explore and visualize Openstack deployments in a dynamic
manner.

## Rationale

Basically, I got frustrated with python and I wanted to get better at clojure.  As a warning, 
this is my first serious clojure project, and I am in many ways learning clojure as I go.

Having worked a little with the python official client (eg python-keystoneclient, etc), and
OpenStack's tempest, I found myself wishing for something a little more rigorous.  Even though
clojure is a dynamic language, it does allow for type hinting (or Typed Clojure aka core.typed).
It also has pre and post condition assertions built into the language.  So shi will endeavour
to use as many assertions as possible such as type hinting, use of pre and post maps, and 
possibly typed clojure in the future.

## Usage

After cloning this project, the first step is to install leinengen.  Leinengen is used as
the build tool for most clojure projects.  You will also need to configure some environment 
variables or Change directory into shi and run the following:

    lein repl

This will provide a clojure based nREPL and will download all the necessary dependencies
for you.  By default, it will run in a shi.testing namespace.

### Configuration

Just like the official python openstack clients like to have environment variables defined
like OS_USERNAME or OS_AUTH_URL, shi follows the same convention.  The shi.environment.config
namespace holds a var called config which will, by default, pull various enviroment variables
in order to create a Credential.  The environment variables which are used are:

- OS_AUTH_URL The url for the keystone authentication endpoint
- OS_USERNAME The user to get authorization for (requires OS_DOMAIN_ID or OS_DOMAIN_NAME)
- OS_USER_ID The user id for which you want to get authorization (required if OS_DOMAIN_ID not given)
- OS_PASSWORD The password or token for the user
- OS_DOMAIN_ID A Keystone v3 domain that the user belongs to (not required for keystone v2.0)
- OS_DOMAIN_NAME A Keystone v3 domain name (not required for keystone v3)
- OS_AUTH_VERSION The keystone version ("v2.0" or "v3" at the moment)

You can also create your own configuration map by calling the shi.environment.config/conf function
and passing in the appropriate key value pairs:

    (require '[shi.environment.config :as sec])
    (require '[shi.api.keystone :as ks])
    (let [config (sec/conf :auth-url "http://192.168.0.100:5000"
                           :user "demo-user"
                           :user-pass "secret"
                           :domain-name "default"
                           :version "v3")]
      (println config)
         credential (ks/make-creds-v3 :userid (config :user-name)
                                      :auth-url (config :auth-url)
                                      :authmethod "password"
                                      :secret (config :user-pass)))]
        

### Authorization

The first thing you will probably want to do is to authenticate yourself to keystone in order
to get the token for your user.  The first step is to create a Credentials or CredentialsV3 
defrecord as shown above, and then passing this to the shi.api.keystone/authorize method:

    (let [config (sec/conf :auth-url "http://192.168.0.100:5000"
                           :user "demo-user"
                           :user-pass "secret"
                           :domain-name "default"
                           :version "v3")]
          credential (ks/make-creds-v3 :userid (config :user-name)
                                       :auth-url (config :auth-url)
                                       :authmethod "password"
                                       :secret (config :user-pass)))]
      (ks/authorize credential))

This returns the all-important map that contains, among other things, the token and service
catalog.  This map (often called auth as an argument to functions) is used everywhere since
the token is required for all REST calls.  It's also used by the shi functions to pull out the
service url endpoint.


## Roadmap

Currently, this project is a one man show and I am working on it as features are needed.  
The project shouldn't even be considered alpha at this point, so use at your own risk.

That being said, additional goals are:

- More unit tests
- More documentation
- More {:pre and :post}
- More type hinting and possibly even core.typed
- More api coverage, especially nova
- Web server (probably using immutant and firefly)


## License

Copyright © 2014 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
