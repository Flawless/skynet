(ns skynet.main
  (:gen-class)
  (:require
   [clojure.string :as s]
   [clojure.tools.cli :refer [parse-opts]]
   [integrant.core :as ig]
   [skynet.core :as sknt]
   [telegrambot-lib.core :as tbot]))

(def cli-options
  ;; An option with a required argument
  [["-a" "--ai-key AI-KEY" "OpenAI key"]
   ["-t" "--token TOKEN" "Telegram bot token"]
   ["-p" "--profile PROFILE" "system profile"
    :parse-fn keyword
    :default :prod
    :validate [#{:dev :prod}]]
   ["-h" "--help"]])

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (s/join \newline errors)))

(defn usage [options-summary]
  (->> ["Tg translator bot Skynet. There are many like it, but this one is desired to conquer the world."
        ""
        "Usage: java -jar sknt.jar [options]"
        ""
        "Options:"
        options-summary]
       (s/join \newline)))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn validate-args
  "Validate command line arguments. Either return a map indicating the program
  should exit (with an error message, and optional ok status), or a map
  indicating the action the program should take and the options provided."
  [args]
  (let [{:keys [options errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options) ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}

      errors ; errors => exit with description of errors
      {:exit-message (error-msg errors)}

      :else
      {:options options})))

(defn -main [& args]
  (let [{:keys [options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (let [{:keys [token ai-key profile]} options
            config (some-> (sknt/config profile)
                           ai-key (assoc-in [:translation/engine :auth] ai-key)
                           token (assoc-in [:tg/bot :token] token))]
        (ig/init config)))))
