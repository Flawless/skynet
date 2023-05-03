(ns skynet.flirt
  (:require [skynet.ai :as ai]))

(defn response [text l opts]
  (let [response (-> (ai/generate-completion [{:role :system :content (str "You are dating dialog helper. Do not add comments. User will provide you text from recognized screenshot, you should threat is like a dialog. Use emoji in text.")}
                                              {:role :user :content (str "Continue the dialog in a flirt manner:\n" text)}]
                                             (assoc opts :temperature 1))
                        (last)
                        :content)]
    (println text "\n=>\n" response)
    response))
