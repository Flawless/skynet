(ns user
  (:require
   [integrant.repl :refer [go halt set-prep!]]
   [integrant.core :as ig]
   [skynet.core :as sknt]
   [telegrambot-lib.core :as tbot]))

(set-prep! sknt/config)

#_(set-refresh-dirs "dev" "src/clj")

(defn system []
  integrant.repl.state/system)

(def reset integrant.repl/reset)

(comment
  (go)
  (halt)
  (def voice
    (first (get-new-messages devbot)))


  (def handler (sknt/handler devbot opts))
  (handler voice)

  (let [{voice-file :file_id
         mime :mime_type} (-> voice :message :voice)
        path (-> devbot
                 (tbot/get-file voice-file)
                 :result
                 :file_path)
#_#_#_#_        {voice :body} (client/get (sknt/file-url path token) {:as :stream})
        ;; file-part (-> voice .getBytes (io/input-stream))
        req {:headers {"Authorization" (str "Bearer " auth)}
             :as :json
             :coerce :always
             :multipart [{:name "file" :content (io/file "voice.wav")}
                         {:name "model" :content "whisper-1"}]}]
    #_(io/copy voice (io/file "voice.ogg"))
    (sknt/ogg->wav (sknt/file-url path tlg-token))
    #_(client/post (skynet.translate/url "audio" "translations") req))

)
(def tlg-token (System/getenv "TG_BOT_TOKEN"))
(def gpt-token (System/getenv "OPENAI_TOKEN"))

(def devbot (tbot/create tlg-token))

(def opts {:auth gpt-token :token tlg-token})

(defonce updates (atom 0))

(defn get-new-messages [bot]
  (let [msgs (-> (tbot/get-updates bot {:offset @updates}):result)]
    (when-some [update-id (some-> msgs peek :update_id inc)]
      (reset! updates update-id))
    msgs))

(comment
  (get-new-messages devbot))
