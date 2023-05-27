(ns skynet.bot.commands
  (:require
   [clojure.tools.logging :as log]
   [skynet.translate :as translate]))

(defmulti handle
  (fn [msg _]
    (-> msg :message :text (subs 1) keyword)))

(defmethod handle :start
  [{{{l :language-code} :from} :message} translations]
  {:text (translate/introduce l translations)})

(defmethod handle :stats
  [{{chat :chat}:message} _translations]
  {:text chat})

(defmethod handle :default
  [_ _]
  {:text "Unknown command"})
