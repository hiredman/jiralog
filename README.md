# jiralog

A Clojure library for bringing JIRA data in to your core.logic
programs

## Usage

    [com.thelastcitadel/jiralog "0.0.1"]

The main thing jiralog adds is the `com.thelastcitadel.jiralog/jira`
relation, which allows you to preform datalog style search via
unification against the JIRA API:

```clojure
(defn example []
  (logic/run* [q]
    (logic/fresh [status k n url components c summary]
      (logic/== q {:name k
                   :story n
                   :status status
                   :component c
                   :summary summary})
      (jira {:fields {:assignee {:displayName k} :labels ["some-label"]}})
      (c/string-containsc summary "Some Thing About Whatever")
      (jira {:fields {:assignee {:displayName k}
                      :status {:name status}
                      :components components
                      :summary summary}
             :self url
             :key n})
      (logic/conde
       [(logic/== status "Open")]
       [(logic/== status "Needs Review")])
      (logic/fresh [x]
        (logic/membero x components)
        (logic/featurec x {:name c})))))
```

The "unification" isn't real unification. For maps it allows for
partial matches, the search succeeds if at minimum the maps JIRA
returns contain at least the given keys and values.

The `jira` relation is configured using the
[carica](https://github.com/sonian/carica) configuration library. It
expects at least the key `:jiralog/url`; optionally `:jiralog/user`
and `:jiralog/password`.

The mechanism used to generate JQL queries from the map structures is
not complete, but it is extensible, see
`com.thelastcitadel.querystring`.

Jiralog can also in some cases use specially created constraints to generate
better queries, see `com.thelastcitadel.constraints/string-containsc`.

If you want to see what kind of queries are being generated you can
checkout `com.thelastcitadel.query/queries` which is an agent that
contains a list of the last 10 queries run, latest first.

I have been using this against a JIRA rest api at an endpoint like
`https://SUBDOMAIN.atlassian.net/rest/api/latest/search`, if your JIRA
has a similar domain this should work for you, if not, who knows, JIRA
is weird. For example the Clojure dev JIRA doesn't return any fields
with search results, so it is pretty useless.

## See also

http://docs.atlassian.com/jira/REST/latest/

https://confluence.atlassian.com/display/JIRA/Advanced+Searching#AdvancedSearching-OperatorsReference
        
## License

Copyright © 2013 Kevin Downey

Distributed under the Eclipse Public License, the same as Clojure.
