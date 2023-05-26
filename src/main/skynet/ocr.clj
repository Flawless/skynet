(ns skynet.ocr
  (:import
   (net.sourceforge.tess4j Tesseract)))

(defn ocr [bi]
  (let [t (new Tesseract)]
    (.doOCR t bi)))
