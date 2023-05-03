(ns skynet.ocr
  (:import
   (net.sourceforge.tess4j Tesseract)))

(defn tesseract
  "An tesseract factory"
  ([] (new Tesseract))
  ([lang]
   (doto (new Tesseract)
     (.setLanguage lang))))

(defn do-ocr [bi tess-instance]
  (.doOCR tess-instance bi))

(defn ocr [bi]
  (let [t (new Tesseract)]
    (.doOCR t bi)))
