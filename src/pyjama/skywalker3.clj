(ns pyjama.skywalker3
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.pprint]
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

(defn rag-folder [config]
  (let [persist-file (or (:embeddings-file config) "embeddings.bin")
        documents
        (if (pic/file-exists? persist-file)
          (pyjama.io.embeddings/load-documents persist-file)
          (let [documents
                (pyjama.io.embeddings/generate-vectorz-folder
                  (select-keys config [:documents :url :chunk-size :embedding-model]) (:documents config) nil)]
            (pyjama.io.embeddings/save-documents persist-file documents)
            documents))

        enhanced-context
        (pyjama.embeddings/enhanced-context
          (assoc
            (select-keys config
                         [:question :url :embedding-model :top-n])
            :documents documents
            ))]
    (pyjama.core/ollama
      url
      :generate
      (assoc
        (select-keys config [:options :stream :model :pre])
        :prompt [enhanced-context (:question config)])
      :response)))


(defn jpx-rag [question]
  (let [folder "../pyjama-skywalker/skywalker20/mds"
        pre "Context: \n\n
        %s.
        \n\n
        Answer the question:
        %s
        using no previous knowledge and ONLY knowledge from the context. No comments.
        If you don't know answer 「わかりません」
        Make the answer as short as possible."
        ]
    (rag-folder {:pre             pre
                 :embeddings-file "jpx.bin"
                 :url             url
                 :model           "llama3.1"
                 :chunk-size      1200
                 :top-n           3
                 :question        question
                 :documents       folder
                 :embedding-model embedding-model})))

;(deftest test-jpx-rag-one
;         (println
;           (jpx-rag "高松コンストラクショングループの2025年3月期の受注高の計画は前期比何倍か、小数第三位を四捨五入し答えてください。")))

(defn -main [& args]
  (let [questions (pyjama.io.core/load-lines-of-file "../pyjama-skywalker/skywalker20/questions.txt")
        output-file "output.csv"]
    (with-open [writer (io/writer output-file)]
      (csv/write-csv writer (map-indexed (fn [idx question]
                                           (println "Ragging:[" idx "] " question)
                                           [(str idx) (jpx-rag question)])
                                         questions)))))