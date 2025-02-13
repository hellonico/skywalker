(ns pyjama.skywalker2
  "Use lucene to search for the proper document. Depending on the strategy insert
  :sentences
  :full text
  :parts
  of the best document in the context to answer the question.
  Requires a large context but works pretty nicely.
  "
  (:gen-class)
  (:require
    [clojure.data.csv :as csv]
    [clojure.java.io :as io]
    [clojure.pprint]
    [pyjama.chatgpt.core]
    [pyjama.functions :refer [ollama-fn]]
    [pyjama.io.core :as pyo]
    [pyjama.io.indexing :refer :all]
    [pyjama.io.print])
  (:import (java.util Date)))

(def ascii "____ _  _ _   _ _ _ _ ____ _    _  _ ____ ____    ___ _ _ _ ____ \n[__  |_/   \\_/  | | | |__| |    |_/  |___ |__/     |  | | | |  | \n___] | \\_   |   |_|_| |  | |___ | \\_ |___ |  \\     |  |_|_| |__| \n                                                                 \n")

(defn- custom-append-to-csv
  "Append objects, prepended with the line number starting from 0"
  [output-file object]
  (when-not (.exists (io/file output-file))
    (spit output-file ""))
  (let [line-number (with-open [reader (io/reader output-file)] (count (csv/read-csv reader)))]
    (with-open [writer (io/writer output-file :append true)]
      (csv/write-csv writer (map #(cons line-number %) object)))))

(defn answer-to-question-with-docs
  "use the clucy index"
  [config question]
  (println "=== " question " ===")
  (let [keywords ((ollama-fn (:keyworder config)) question)
        _ (println keywords)
        best-pdf (:doc (best-matching-document keywords))

        augmented (augmented-text best-pdf keywords (:augmented-strategy config))

        ; Ollama
        answer ((ollama-fn (:answerer config)) [augmented question])
        use-shortener (not (nil? (:shortener config)))

        shortener (if use-shortener ((ollama-fn (:shortener config)) answer) answer)

        ; ChatGPT works too
        ;answer (pyjama.chatgpt.core/chatgpt
        ;         {:model  "gpt-4o"
        ;          :pre    "以下の文が与えられています: %s. できるだけ正確に質問 %s に日本語で答えてください。文書のサイズは200文字以下。"
        ;          :prompt [augmented question]})
        ]

    (println best-pdf)
    ;
    ;(println "*****")
    ;(println augmented)
    ;(println "*****")

    (println answer)

    (println "Shortener:" use-shortener)
    (println shortener)

    [question best-pdf shortener]
    )
  )


(defn play
  ([{:keys [question-file output-file] :as config}]
   (let [questions (pyjama.io.core/load-lines-of-file question-file (or (:start config) 0))]
     (doseq [question questions]
       (->> question
            (answer-to-question-with-docs config)
            ((fn [answers] (custom-append-to-csv output-file [[(nth answers 2)]]))))))))

(defn replay
  ([{:keys [question-file output-file replay] :as config}]
   (let [existing-answers (with-open [reader (io/reader output-file)]
                            (doall (csv/read-csv reader)))
         ;output-file (if replay (str output-file ".replay.csv") output-file)
         questions (pyjama.io.core/load-lines-of-file question-file)
         ]
     (when (.exists (io/as-file output-file))
       (spit output-file ""))                               ; blank replay file
     (doseq [[idx question] (map-indexed vector questions)]
       (println "Replay ->" idx)
       (if (or (empty? replay) (some #(= idx %) replay))
         (->> question
              (answer-to-question-with-docs config)
              ((fn [answers] (custom-append-to-csv output-file [[(nth answers 2)]]))))
         (custom-append-to-csv output-file [[(second (nth existing-answers idx))]]))))))

(defn cag [config]
  (if (or
        (not (.exists (io/as-file index-path)))
        (:index config))
    (index-documents (:folder-path config)))
  (if (:replay config)
    (replay config) (play config)))

(defn -main [& args]
  (let [config-file (or (first args) "skywalker20/settings.edn")
        settings (pyo/read-settings config-file)
        settings (assoc settings
                   :start-time (Date.)
                   :config-file config-file)]
    (println ascii "\n")
    (pyjama.io.print/pretty-print-map settings)
    (cag settings)))