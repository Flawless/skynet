(ns user
  (:require
   [clojure.core.async :as async]
   [clojure.java.io :as io]
   [integrant.repl :refer [go halt set-prep!]]
   [skynet.bot.handler :as handler]
   [skynet.core :as sknt]
   [skynet.ocr :as ocr]
   [telegrambot-lib.core :as tbot])
  (:import
   (javax.imageio ImageIO)
   (net.sourceforge.tess4j Tesseract)))

(set-prep! sknt/config)

#_(set-refresh-dirs "dev" "src/clj")

(defn system []
  integrant.repl.state/system)

(def reset integrant.repl/reset)

(comment
  (sknt/config)
  (go)
  (reset)
  (halt)
  (def voice
    (first (get-new-messages devbot)))

  (:bot-token devbot)


  (def handler (handler/handler devbot opts))
  (async/go
    (handler (first newmsgs)))

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

(defn file-url [path token]
  (format "https://api.telegram.org/file/bot%s/%s" token path))

(defn- get-url [bot id opts]
  (-> (tbot/get-file bot id) :result :file_path (file-url (-> :token opts))))

(comment
  (def newmsgs (get-new-messages devbot))
  newmsgs
  ;; => [{:update_id 102715731,
  ;;      :message
  ;;      {:message_id 165,
  ;;       :from
  ;;       {:id 5822055895,
  ;;        :is_bot false,
  ;;        :first_name "Leafy Reddish",
  ;;        :last_name "Vegetable",
  ;;        :username "C11H26NO2PS",
  ;;        :language_code "en"},
  ;;       :chat
  ;;       {:id 5822055895, :first_name "Leafy Reddish", :last_name "Vegetable", :username "C11H26NO2PS", :type "private"},
  ;;       :date 1683115594,
  ;;       :photo
  ;;       [{:file_id "AgACAgIAAxkBAAOlZFJOSlt6VavfWaYilQ3pGilDSmYAAofGMRt-25hKdTlZ3rAqoFcBAAMCAANzAAMvBA",
  ;;         :file_unique_id "AQADh8YxG37bmEp4",
  ;;         :file_size 665,
  ;;         :width 90,
  ;;         :height 24}
  ;;        {:file_id "AgACAgIAAxkBAAOlZFJOSlt6VavfWaYilQ3pGilDSmYAAofGMRt-25hKdTlZ3rAqoFcBAAMCAANtAAMvBA",
  ;;         :file_unique_id "AQADh8YxG37bmEpy",
  ;;         :file_size 4821,
  ;;         :width 320,
  ;;         :height 85}
  ;;        {:file_id "AgACAgIAAxkBAAOlZFJOSlt6VavfWaYilQ3pGilDSmYAAofGMRt-25hKdTlZ3rAqoFcBAAMCAAN4AAMvBA",
  ;;         :file_unique_id "AQADh8YxG37bmEp9",
  ;;         :file_size 7132,
  ;;         :width 480,
  ;;         :height 128}]}}]

  ;;
  )

(defn tesseract
  "An tesseract factory"
  ([] (new Tesseract))
  ([lang]
   (doto (new Tesseract)
     (.setLanguage lang))))

;; param 1 -> buffered image ( to read a file from disk to buffered image is (ImageIO/read (clojure.java.io/file "pic.png")) )
;; param 2 -> Tesseract instance
;; returns -> text from the image
(defn do-ocr [bi tess-instance]
  (.doOCR tess-instance bi))

(comment

  (def tess (tesseract "eng"))
  (def bi (ImageIO/read (clojure.java.io/file (clojure.java.io/resource "pic.png"))))
  (def bi (clojure.java.io/file (clojure.java.io/resource "pic.png")))
  (def url (get-url devbot  "AgACAgIAAxkBAAOlZFJOSlt6VavfWaYilQ3pGilDSmYAAofGMRt-25hKdTlZ3rAqoFcBAAMCAAN4AAMvBA" {:token tlg-token}))

  (io/copy (io/file url) (io/file "new"))

  url
  ;; => "https://api.telegram.org/file/bot6002596263:AAEu7V3gFE_T9QseKq4BAge8tcqP_4bDoEI/photos/file_17.jpg"


  (ocr/ocr (ImageIO/read (io/as-url url)))


  (slurp 123)
  (.exists (io/file "http://clojuredocs.org/clojure.java.io/file"))
  (.exists (io/as-url "http://clojuredocs.org/clojure.java.io/file"))

  (do-ocr
   (io/reader url)
   (tesseract))
  ;;
  )
