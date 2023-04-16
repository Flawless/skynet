(ns skynet.bot.handler
  (:require
   [clojure.java.io :as io]
   [clojure.string :as s]
   [kawa.core :as kawa]
   [skynet.translate :as translate]
   [telegrambot-lib.core :as tbot]))

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

(defn- handle-voice [bot {voice-id :file_id} opts l chat-id]
  (let [file-url (-> (tbot/get-file bot voice-id) :result :file_path (file-url (-> :token opts)))
        wav-file (ogg->wav file-url)]
    (try
      (let [transcription (translate/transcribe wav-file opts)
            translation (translate/translate transcription l opts)]
        {:chat-id chat-id
         :text (s/join "\n\n" [transcription translation])})
      (finally (io/delete-file wav-file)))))

(defn handler [bot translations]
  (fn
    [{{{chat-id :id} :chat
       text :text
       {l :language_code} :from
       voice :voice} :message :as msg}]
    (cond
      (some-> text (s/starts-with? "/start"))
      {:chat-id chat-id :text (translate/introduce l translations)}

      (some? voice)
      (handle-voice bot voice translations l chat-id)

      :else
      {:chat-id chat-id
       :text (translate/translate text l translations)})))
