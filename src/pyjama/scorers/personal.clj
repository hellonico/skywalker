(ns pyjama.personal
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [pyjama.functions]))


(def scorer-config
  {
   :url   "http://localhost:11432"
   :model "llama3.1"
   :format
   {
    ; https://json-schema.org/understanding-json-schema/reference/numeric#range
    :items {:type       "object"
             :required   [:score :category]
             :properties {:category {:type "string" :enum [:good :bad :perfect]} :score {:type "integer" :minimum 0 :maximum 100}}}
    :pre    "Given the question %s, score the relevance of answer based only the question %s. Also give a category."
    :stream false}})

(defn scorer [question answer]
  ((pyjama.functions/ollama-fn scorer-config) question answer))

(defn process-files [question-file csv-file output-file]
  (with-open [wtr (io/writer output-file)]
    (csv/write-csv wtr [])
    (let [questions (line-seq (io/reader question-file))
          csv-reader (io/reader csv-file)]
      (doseq [[q [idx a]] (map vector questions (rest (csv/read-csv csv-reader)))]
        (let [[score1 score2] (scorer q a)]
          (csv/write-csv wtr [(vector score1 score2 a q)]))
        (.flush wtr)))))

(defn -main [& args]
  (process-files "skywalker03/questions.txt" "skywalker03/final-01.csv" "skywalker03/scores.csv")
  )