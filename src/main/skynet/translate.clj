(ns skynet.translate
  (:require
   [clj-http.client :as client]
   [clojure.java.io :as io]
   [clojure.string :as s]
   [clojure.tools.logging :as log]))

(defn url [& path]
  (s/join "/" (into ["https://api.openai.com" "v1"] path)))

(def default-chat-model "gpt-3.5-turbo")

(defn chat-form [conversation {:keys [max-tokens temperature model]
                               :or {max-tokens 2048
                                    temperature 0
                                    model default-chat-model}}]
  {:model model
   :messages conversation
   :max_tokens max-tokens
   :temperature temperature})

(defn- gen-bearer [auth]
  (str "Bearer " auth))

(defn wrap-request
  ([{:keys [^String auth]}]
   {:headers {"Authorization" (gen-bearer auth)}
    :as :json
    :coerce :always
    :content-type :json})

  ([form {:keys [^String auth]}]
   {:headers {"Authorization" (gen-bearer auth)}
    :as :json
    :coerce :always
    :content-type :json
    :form-params form}))

(defn- -generate-completion [conversation opts]
  (let [request (-> (chat-form conversation opts)
                    (wrap-request opts))
        _ (log/debug (str ">>ai " request))
        response (client/post (url "chat" "completions")
                              request)
        _ (log/debug (str "<<ai " response))]
    (conj conversation
          (-> response
              (get-in [:body :choices 0 :message])
              (update :role keyword)))))

(defn transcribe [file {:keys [auth]}]
  (println :exists? (.exists (io/file file)))
  (let [request (-> {:headers {"Authorization" (gen-bearer auth)}
                     :multipart [{:name "file" :content (io/file file)}
                                 {:name "model" :content "whisper-1"}]
                     :as :json
                     :coerce :always
                     :save-request? true})]
    (Thread/sleep 100)
    (-> (client/post (url "audio" "transcriptions") request)
               :body
               :text)))

(defn introduce [lang opts]
  (-> (-generate-completion [{:role :system :content (str "You are translator bot and should translate each user message to desired language (English if not specified otherwise).")}
                             {:role :user :content (format "Please, introduce yourself as an translation bot in language defined by locale \"%s\" and give a brief instruction. Also note, that you can process voice messages along with texts." lang)}]
                            opts)
      (last)
      :content))

(defn translate [text l opts]
  (let [translation (-> (-generate-completion [{:role :system :content (str "You are tranlator and should translate each user message to desired language (English if not specified otherwise). All your notes should be providen in " l)}
                                               {:role :user :content (str "Translate:\n" text)}]
                                              opts)
                        (last)
                        :content)]
    (println text "\n=>\n" translation)
    translation))
