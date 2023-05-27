(ns skynet.bot.handler
  (:require
   [clojure.java.io :as io]
   [clojure.stacktrace]
   [clojure.string :as s]
   [kawa.core :as kawa]
   [skynet.ocr :as ocr]
   [skynet.translate :as translate]
   [telegrambot-lib.core :as tbot])
  (:import
   (javax.imageio ImageIO)))

(def admin? #{375758278 5371398693 5822055895})

(defn file-url [path token]
  (format "https://api.telegram.org/file/bot%s/%s" token path))

;; FIXME my eyes, please, somebody make it better
(defn ogg->wav [file-url]
  (let [filename (str "/tmp/" (gensym) ".wav")]
    (kawa/ffmpeg! :input-url file-url filename :acodec "libvirbis")
    (loop [i 100]
      (Thread/sleep 100)
      (when (> i 0)
        (if (.exists (io/file filename))
          filename
          (recur (dec i)))))))

(defn- get-url [bot id]
  (-> (tbot/get-file bot id) :result :file_path (file-url (:bot-token bot))))

(defn- handle-voice [bot {voice-id :file_id} opts l chat-id]
  (let [file-url (get-url bot voice-id)
        wav-file (ogg->wav file-url)]
    (try
      (let [transcription (translate/transcribe wav-file opts)
            translation (translate/translate transcription l opts)]
        {:chat-id chat-id
         :text (s/join "\n\n" [transcription translation])})
      (finally (io/delete-file wav-file)))))

(defn- handle-img [bot photo-coordinates opts l chat-id]
  (let [img-maxres-id (:file_id (first (sort-by :file_size > photo-coordinates)))
        url (get-url bot img-maxres-id)
        bi (ImageIO/read (io/as-url url))
        text (ocr/ocr bi)
        respose (translate/translate text l opts)]
    {:chat-id chat-id
     :text respose}))

(defn handler [bot translations]
  (fn
    [{{{chat-id :id} :chat
       text :text
       {l :language_code} :from
       voice :voice
       photo :photo} :message :as msg}]
    (try
      (cond
        (some-> text (s/starts-with? "/start"))
        {:chat-id chat-id :text (translate/introduce l translations)}

        (some? voice)
        (handle-voice bot voice translations l chat-id)

        (some? photo)
        (handle-img bot photo translations l chat-id)

        :else
        {:chat-id chat-id
         :text (translate/translate text l translations)})
      (catch Exception e
        (if (admin? chat-id)
            {:chat-id chat-id
             :text (with-out-str
                     (clojure.stacktrace/print-stack-trace e))}
            {:chat-id chat-id
             :text "Something went wrong. Please, try again or contact my master @AlexanderUshanov"})))))
