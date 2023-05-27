(ns skynet.core
  (:require
   [skynet.bot.core]
   [skynet.nrepl]
   [aero.core :as aero]
   [clojure.java.io :as io]
   [integrant.core :as ig]))

(defmethod aero/reader 'ig/ref [_ _ value]
  (ig/ref value))

(defn config
  ([] (config :dev))
  ([profile]
   (-> "config.edn"
       io/resource
       (aero/read-config {:profile profile}))))
