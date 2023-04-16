(ns build
  (:require
   [clojure.tools.build.api :as b]))

(def lib 'flawless/sknt)
(def version "0.0.0" #_(format "0.0.%s" (b/git-count-revs nil)))
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def uber-file (format "target/%s-%s-standalone.jar" (name lib) version))

(defn clean [_]
  (b/delete {:path "target"}))

(defn uber [_]
  (clean nil)
  (b/copy-dir {:src-dirs ["src/main" "resources"]
               :target-dir class-dir})
  (b/compile-clj {:basis basis
                  :java-cmd "java"
                  :src-dirs ["src/main"]
                  :class-dir class-dir})
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis basis
           :main 'skynet.main})
  (println "Successfully built" uber-file))
