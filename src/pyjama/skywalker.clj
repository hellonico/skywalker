(ns pyjama.skywalker
  (:gen-class)
  (:require [clojure.java.io :as io]
            [pyjama.best]
            [pyjama.core]
            [pyjama.functions]
            [pyjama.io.cache]
            [pyjama.io.print]
            [pyjama.io.core :as pyo]
            [pyjama.io.readers])
  (:import (java.util Date)))

;(alter-var-root #'pyjama.io.readers/extract-text memoize)

(alter-var-root #'pyjama.io.readers/extract-text
                (fn [original-fn]
                  (pyjama.io.cache/memoize-to-sqlite "extract-text" original-fn)))

(defn run [{:keys [folder reuse-scoring] :as settings}]
  (let [questions (pyo/load-lines-of-file (or (:questions settings) (str folder "/questions.txt")))
        documents (pyo/load-files-from-folders (:docs settings) #{"docx" "pdf" "txt" "epub" "md"})
        best-file (str folder "/best.edn")
        best-docs (if (and reuse-scoring (.exists (io/as-file best-file))) ; reuse a previous scoring run
                    (pyo/load-best-documents (str folder "/best.edn"))
                    (->>
                      (pyjama.best/best-documents
                        questions
                        documents
                        pyjama.io.readers/extract-text
                        settings)
                      (pyo/save-to-file best-file)))
        answers (pyjama.best/get-answers
                  best-docs
                  pyjama.io.readers/extract-text
                  settings)
        _ (pyo/save-to-file (str folder "/answers.edn") answers)
        ]
    (pyo/save-to-csv (str folder "/output.csv") answers ["question" "document used" "answer"])
    (shutdown-agents)
    (println (slurp (str folder "/output.csv")))))

(def ascii
  "____ _  _ _   _ _ _ _ ____ _    _  _ ____ ____ \n[__  |_/   \\_/  | | | |__| |    |_/  |___ |__/ \n___] | \\_   |   |_|_| |  | |___ | \\_ |___ |  \\ \n                                               ")


(def sample-settings
  {:url           (or (System/getenv "OLLAMA_URL")
                      "http://localhost:11434")
   ;:scoring-model   (or
   ;                   (System/getenv "SKYWALKER_MODEL_1")
   ;                   (System/getenv "OLLAMA_MODEL")
   ;                   "llama3.2")
   :score         {:format {:type "object" :properties {:score {:type "integer"}}}
                   :model  "llama3.2"
                   :pre    "Read carefully this document %s.\n How relevant is the text to answer the following question, read question carefully and be aware of context: %s. Give a score between 1 and 100."}
   :docs          "skywalker01/docs"
   :trace         true
   ;:answering-model (or
   ;                   (System/getenv "SKYWALKER_MODEL_2")
   ;                   (System/getenv "OLLAMA_MODEL")
   ;                   "llama3.2")
   :answer        {
                   :model "llama3.2"
                   :pre   "This is a text %s.\n Give me answer for following question after reading this document, read question carefully, and be aware of context, in one line and in less than 200 characters: %s"}
   :reuse-scoring (or (System/getenv "SKYWALKER_SKIP") false)
   :folder        "skywalker01"}
  )

(defn -main [& args]
  (let [settings (or (pyo/read-settings (first args))
                     {:url             (or (System/getenv "OLLAMA_URL")
                                           "http://localhost:11434")
                      :scoring-model   (or
                                         (System/getenv "SKYWALKER_MODEL_1")
                                         (System/getenv "OLLAMA_MODEL")
                                         "llama3.2")
                      :trace           true
                      ;:score {:format {:type "object" :properties {:score {:type "integer"}}}
                      ;         :model  "llama3.2
                      ;         :pre    "Read carefully this document %s.\n How relevant is the text to answer the following question, read question carefully and be aware of context: %s. Give a score between 1 and 100."}
                      :docs            "skywalker01/docs"
                      :answering-model (or
                                         (System/getenv "SKYWALKER_MODEL_2")
                                         (System/getenv "OLLAMA_MODEL")
                                         "llama3.2")
                      ;:answer {
                      ;         :model "llama3.2"
                      ;         :pre   "This is a text %s.\n Give me answer for following question after reading this document, read question carefully, and be aware of context, in one line and in less than 200 characters: %s"}
                      :reuse-scoring   (or (System/getenv "SKYWALKER_SKIP") (Boolean/parseBoolean (second args)) false)
                      :folder          (or (first args) "skywalker01")})
        settings (assoc settings :start-time (Date.))
        ]
    (println ascii "\n")

    (if (.equalsIgnoreCase "--help" (first args))
      (do
        (println "Generating sample run file: sample.edn")
        (pyo/pprint-to-file "sample.edn" sample-settings)
        (clojure.pprint/pprint (pyo/read-settings "sample.edn")))
      (do
        ;(clojure.pprint/pprint settings)
        (pyjama.io.print/pretty-print-map settings)
        (run settings)))))