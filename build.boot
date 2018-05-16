(set-env!
 :source-paths    #{"src/clj" "src/cljs"}
 :resource-paths  #{"resources"}
 :dependencies '[[org.clojure/clojure          "1.8.0"]
                 [org.clojure/clojurescript    "1.9.229"]
                 [com.cognitect/transit-clj    "0.8.290" :exclusions [com.fasterxml.jackson.core/jackson-core
                                                                      com.fasterxml.jackson.core/jackson-databind]]
                 [com.cognitect/transit-cljs   "0.8.239"]
                 [compojure                    "1.5.1"]
                 [http-kit                     "2.2.0"]
                 [ring                         "1.6.0-beta6"]
                 [rum                          "0.10.7"]
                 [mount                        "0.1.11"]
                 [cprop                        "0.1.9"]
                 [tolitius/mount-up            "0.1.0-SNAPSHOT"]
                 [tolitius/envoy               "0.1.2-SNAPSHOT"]
                 [counsyl/vault-clj            "0.2.0"]
                 [chazel                       "0.1.16"]
                 [org.clojure/tools.logging    "0.3.1"]

                 ;; dev / test
                 [adzerk/boot-cljs             "1.7.228-2"       :scope "test"]
                 [adzerk/boot-cljs-repl        "0.3.3"           :scope "test"]
                 [adzerk/boot-reload           "0.4.13"          :scope "test"]
                 [pandeiro/boot-http           "0.7.2"           :scope "test"]
                 [adzerk/boot-logservice       "1.2.0"           :scope "test"]
                 [com.cemerick/piggieback      "0.2.1"           :scope "test"]
                 [org.clojure/tools.nrepl      "0.2.12"          :scope "test"]
                 [weasel                       "0.7.0"           :scope "test"]
                 [crisptrutski/boot-cljs-test  "0.2.0-SNAPSHOT"  :scope "test"]])

(require
 '[adzerk.boot-cljs      :refer [cljs]]
 '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
 '[adzerk.boot-reload    :refer [reload]]
 '[pandeiro.boot-http    :refer [serve]]
 '[adzerk.boot-logservice :as log-service]
 '[clojure.tools.logging :as log]
 '[crisptrutski.boot-cljs-test :refer [test-cljs]]

 '[hubble.env :as env]
 '[hubble.app :as app])

(def log4b
  [:configuration
   [:appender {:name "STDOUT" :class "ch.qos.logback.core.ConsoleAppender"}
    [:encoder [:pattern "%-5level %logger{36} - %msg%n"]]]
   [:root {:level "TRACE"}
    [:appender-ref {:ref "STDOUT"}]]])

(deftask build []
  (comp (speak)

        (cljs)
        ))

(deftask run []
  (app/-main)
  (comp (watch)
        ;; (cljs-repl)
        (reload)
        (build)))

(deftask production []
  (task-options! cljs {:optimizations :advanced})
  identity)

(deftask development []
  (set-env! :resource-paths #(conj % "dev-resources"))
  (task-options! cljs {:optimizations :none :source-map true}
                 reload {:on-jsload 'hubble.app/init})

  (alter-var-root #'log/*logger-factory*
                  (constantly (log-service/make-factory log4b)))
  identity)

(deftask dev []
  (comp (development)
        (repl)))

(deftask init-consul []
  (env/init-consul))

(deftask up []
  (comp (development)
        (run)))

(deftask testing []
  (set-env! :source-paths #(conj % "test/cljs"))
  identity)

;;; This prevents a name collision WARNING between the test task and
;;; clojure.core/test, a function that nobody really uses or cares
;;; about.
(ns-unmap 'boot.user 'test)

(deftask test []
  (comp (testing)
        (test-cljs :js-env :phantom
                   :exit?  true)))

(deftask auto-test []
  (comp (testing)
        (watch)
        (test-cljs :js-env :phantom)))
