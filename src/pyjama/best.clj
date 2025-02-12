(ns pyjama.best
  (:require [pyjama.functions])
  (:import (java.util Date)))

(defn answer-question-with-text [settings]
  (pyjama.functions/ollama-fn
    (or (:answer settings)
        {
         :url   (:url settings)
         :model (:answering-model settings)
         :pre   "This is a text %s.\n Give me answer for following question after reading this document, read question carefully, and be aware of context, in one line and in less than 200 characters: %s"})))

(defn score-text-for-question [settings]
  (pyjama.functions/ollama-fn
    (or (:score settings)
        {:format {:type "object" :properties {:score {:type "integer"}}}
         :model  (:scoring-model settings)
         :url    (:url settings)
         :pre    "Read carefully this document %s.\n How relevant is the text to answer the following question, read question carefully and be aware of context: %s. Give a score between 1 and 100."})))

(defn best-documents
  "Give a score to each documents, related to the question."
  [questions documents extract-text-fn settings]
  (let [urls (clojure.string/split (:url settings) #",")    ;; Always a vector of URLs
        url-count (count urls)
        score-fn (score-text-for-question settings)
        process-docs (fn [question-group]
                       (reduce (fn [best-map question]
                                 (let [best-doc
                                       (->> documents
                                            (map-indexed (fn [i doc]
                                                           (let [url (nth urls (mod i url-count))] ;; Round-robin URL selection
                                                             (if (:trace settings)
                                                               (spit (str (:folder settings) "/run.log")
                                                                     (str "score:\t" question "," doc "\n")
                                                                     :append true))
                                                             {:doc  doc
                                                              :date (Date.)
                                                              :score
                                                              (get (score-fn {:url url :prompt [(extract-text-fn doc) question]})
                                                                   :score 0)})))
                                            (apply max-key :score))]
                                   (assoc best-map question best-doc)))
                               {} question-group))]
    (apply merge (pmap process-docs (partition-all url-count questions))))) ;; Parallel processing

(defn get-answers
  [best-docs extract-text-fn settings]
  (let [urls (clojure.string/split (:url settings) #",")    ;; Always a vector of URLs
        url-count (count urls)
        answer-fn (answer-question-with-text settings)
        process-docs (fn [doc-group]
                       (map-indexed
                         (fn [i [question {:keys [doc]}]]
                           (let [url (nth urls (mod i url-count))] ;; Round-robin URL selection
                             (if (:trace settings)
                               (spit (str (:folder settings) "/run.log")
                                     (str "answer:\t" question "," doc "\n")
                                     :append true))
                             [question doc
                              (answer-fn {:url url :prompt [(extract-text-fn doc) question]})]))
                         doc-group))]
    (apply concat (pmap process-docs (partition-all url-count best-docs))))) ;; Parallel processing
