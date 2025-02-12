(ns pyjama.skywalker3
  "Use regular embeddings on the documents, then run a standard RAG."
  (:gen-class)
  (:require [clojure.pprint]
            [clojure.test :refer :all]
            [pyjama.core]
            [pyjama.io.core :as pic]
            [pyjama.io.embeddings]
            [pyjama.io.indexing]
            [pyjama.io.readers]))


(def url (or (System/getenv "OLLAMA_URL")
             "http://localhost:11432"))
;(def embedding-model
;  "mxbai-embed-large")

(def embedding-model "nomic-embed-text")

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
                :model           "mistral"
                :chunk-size      600
                :top-n           5
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

(defn -main [& args]
  (let [questions (pyjama.io.core/load-lines-of-file "../pyjama-skywalker/skywalker20/questions.txt")
        output-file "output.csv"]
    (pyjama.io.core/save-to-csv
      output-file
      (map-indexed
        (fn [idx question]
          (println "Ragging:[" idx "] " question)
          [(str idx) (jpx-rag question)])
        questions))))