(ns skynet.translate
  (:require
   [clj-http.client :as client]
   [clojure.java.io :as io]
   [skynet.ai :as ai]))

(defn transcribe [file {:keys [auth]}]
  (println :exists? (.exists (io/file file)))
  (let [request (-> {:headers {"Authorization" (ai/gen-bearer auth)}
                     :multipart [{:name "file" :content (io/file file)}
                                 {:name "model" :content "whisper-1"}]
                     :as :json
                     :coerce :always
                     :save-request? true})]
    (Thread/sleep 100)
    (-> (client/post (ai/url "audio" "transcriptions") request)
               :body
               :text)))

(defn introduce [lang opts]
  (-> (ai/generate-completion [{:role :system :content (str "You are translator bot and should translate each user message to desired language (English if not specified otherwise).")}
                             {:role :user :content (format "Please, introduce yourself as an translation bot in language defined by locale \"%s\" and give a brief instruction. Also note, that you can process voice messages along with texts." lang)}]
                            opts)
      (last)
      :content))

(defn translate [text l opts]
  (let [translation (-> (ai/generate-completion [{:role :system :content (str "You are translator and should translate each user message to desired language (English if not specified otherwise). All your notes should be providen in " l)}
                                               {:role :user :content (str "Translate:\n" text)}]
                                              opts)
                        (last)
                        :content)]
    (println text "\n=>\n" translation)
    translation))
