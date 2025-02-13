(ns pyjama.skywalker3
  "Use regular embeddings on the documents, then run a standard RAG."
  (:gen-class)
  (:require [clojure.pprint]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [pyjama.core]
            [pyjama.io.core]
            [pyjama.io.embeddings]
            [pyjama.io.indexing]
            [pyjama.io.readers])
  (:import (java.time LocalDateTime)
           (java.time.format DateTimeFormatter)))


(def url (or (System/getenv "OLLAMA_URL")
             "http://localhost:11432"))
(def embedding-model
  "nomic-embed-text")

(defn jpx-rag [question]
  (let [pre "Context: \n\n
        %s.
        \n\n
        Answer the question:
        %s
        using no previous knowledge and ONLY knowledge from the context. No comments.
        If you know the answer, make your answer as short as possible."

        config {:pre             pre
                :embeddings-file "jpx.bin"
                :url             url
                :model           "llama3.1"
                :chunk-size      1000
                :strategy        :cosine
                :top-n           10
                ;:debug           true
                :question        question
                :stream          false
                :documents       "../pyjama-skywalker/skywalker20/mds"
                :embedding-model embedding-model}

        documents (pyjama.io.embeddings/load-documents config)

        ]
    (pyjama.embeddings/rag-with-documents config documents)))

(deftest test-jpx-rag-one
  (println
    (jpx-rag "高松コンストラクショングループの2025年3月期の受注高の計画は前期比何倍か、小数第三位を四捨五入し答えてください。")))

(defn generate-filename []
  (let [timestamp (LocalDateTime/now)
        formatter (DateTimeFormatter/ofPattern "yyyyMMddHHmmss")
        formatted-time (.format timestamp formatter)]
    (str "predictions." formatted-time ".csv")))

(defn -main [& args]
  (let [questions (pyjama.io.core/load-lines-of-file "../pyjama-skywalker/skywalker20/questions.txt")
        output-file (generate-filename)
        answers (map-indexed
                  (fn [idx question]
                    (let [kotae (jpx-rag question) kotae_ (str/replace kotae "\n" ".")]
                      (println "Ragging:[" idx "] " question "\n<>" kotae_)
                      [(str idx) (str/replace kotae_ "\n" ".")]))
                  questions)
        ]
    (pyjama.io.core/save-to-csv output-file answers)))