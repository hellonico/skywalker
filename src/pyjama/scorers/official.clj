(ns pyjama.scorers.official
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]))

(defn filter-ids [file-path]
  (with-open [reader (io/reader file-path)]
    (->> (csv/read-csv reader)
         (filter #(or (not= "Perfect" (nth % 1))
                      (< (Integer/parseInt (nth % 2)) 30)))
         (map first)
         doall)))

(defn -main [& args]
  (-> "skywalker20/02_scoring.csv"
      (filter-ids)
      (vec)
      (println)))