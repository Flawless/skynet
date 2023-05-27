(ns skynet.nrepl
  (:require
   [clojure.tools.logging :as log]
   [integrant.core :as ig]
   [nrepl.server :as nrepl]))

(defmethod ig/init-key :nrepl/server
  [_ {:keys [port]}]
  (let [server (nrepl/start-server :port port)
        _      (log/info "nREPL Server started on port" port)]
    server))

(defmethod ig/halt-key! :nrepl/server
  [_ server]
  (nrepl/stop-server server))
