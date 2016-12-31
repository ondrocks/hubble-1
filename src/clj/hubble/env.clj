(ns hubble.env
  (:require [mount.core :refer [defstate]]
            [cprop.core :refer [load-config]]
            [cprop.source :refer [from-system-props 
                                  from-env]]
            [envoy.core :as envoy]
            [hubble.tools.vault :as vault]
            [hubble.tools.consul :as consul]))

(defstate config :start (-> (load-config :merge [(from-system-props)
                                                 (from-env)])
                            (consul/merge-config [:consul])))




;; playground

(defn init-consul
  "load config to consul without consul props and creds
   this is done once (usually before the app is deployed),
   and here is mostly for demo / repro purposes"
  []
  (let [{:keys [] {host :host
                   kv :kv-prefix} :consul :as conf} (load-config)
        cpath (str host kv)]
    (as-> (dissoc conf :consul) $
      (update-in $ [:hubble :audit] dissoc :auth-token)
      (update-in $ [:hubble :audit :hazelcast] dissoc :group-name :group-password)
      (envoy/map->consul cpath $))))