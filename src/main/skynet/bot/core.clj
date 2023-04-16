(ns skynet.bot.core
  (:require
   [clojure.core.async :as async :refer [<! <!! >! go-loop]]
   [clojure.tools.logging :as log]
   [integrant.core :as ig]
   [telegrambot-lib.core :as tbot]
   [skynet.bot.handler :as handler]))

(defn poll-updates
  "Long poll for recent chat messages from Telegram."
  ([bot]
   (poll-updates bot nil))

  ([bot offset]
   (let [resp (tbot/get-updates bot {:offset offset
                                     :timeout 1000})]
     (if (contains? resp :error)
       (log/error "tbot/get-updates error:" (:error resp))
       resp))))

(defn bot-loop
   "Retrieve and process chat messages."
  [bot handler update-id]
   (log/info "bot service started.")
   (let [exit? (atom false)
         rx-q (async/chan 20)
         exit-chan-a (async/chan)
         exit-chan-b (async/chan)
         tx-q (async/chan 20)
         pipe (async/pipeline 12 tx-q (map handler) rx-q)
         set-id! (fn set-id! [id] (reset! update-id id))]
     (go-loop []
       (log/info "checking for chat updates.")
       (let [updates (poll-updates bot @update-id)
             messages (:result updates)]
         (doseq [msg messages]
           (-> msg :update_id (inc) (set-id!))
           (>! rx-q msg)))
       (if @exit?
         (do
           (log/info "Shutdown receiving loop")
           (>! exit-chan-a :exit))
         (recur)))
     (go-loop []
       (log/info "Procesing message.")
       (let [{:keys [chat-id text]} (<! tx-q)]
         (tbot/send-message bot chat-id text))
       (if @exit?
         (do
           (>! exit-chan-b :exit)
           (log/info "Shutdown processing loop"))
         (recur)))
     {:shutdown (fn []
                  (reset! exit? true)
                  (async/close! pipe)
                  (async/close! rx-q)
                  (async/close! tx-q)
                  ;; wait for loops to shutdown
                  (<!! exit-chan-a)
                  (<!! exit-chan-b))
      :update-id update-id}))

(defmethod ig/init-key :tg/bot
  [_ {:keys [token]}]
  (tbot/create token))

(defmethod ig/init-key :bot/loop
  [_ {:keys [bot handler]}]
  (let [loop (bot-loop bot handler (atom nil))]
    (log/info "Bot loop launched!")
    loop))

(defmethod ig/halt-key! :bot/loop [_ {:keys [shutdown]}]
  (shutdown)
  (log/info "Bot loop gracefully terminated!"))

(defmethod ig/init-key :translations/engine
  [_ opts]
  opts)

(defmethod ig/init-key :bot/handler
  [_ {:keys [bot translations]}]
  (handler/handler bot translations))

(defmethod ig/init-key :http/handler
  [_ _])

(defmethod ig/init-key :jetty/server
  [_ _])

(defmethod ig/init-key :nrepl/server
  [_ _])

(defmethod ig/init-key :jdbc/connection
  [_ _])
