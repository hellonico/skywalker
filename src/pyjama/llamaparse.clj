(ns pyjama.llamaparse
  (:require
    [pyjama.io.core :as pyo]
    [pyjama.llamaparse.core :refer [llama-parser wait-and-download]]))

(defn -main [& args]
  (let [
        settings {:docs "skywalker03/docs2"}
        pdfs (pyo/load-files-from-folders (:docs settings) #{"pdf"})
        ;
        ids [
               "897f68d2-3047-4811-a453-74202a150a4e"
               "130bab9a-0773-4212-af32-b59de620eae2"

               "4391c735-1e94-4322-a180-3938ef878ceb"
               "4b580e8f-d00e-45e4-bca5-064135a12626"

             ]
        ]
    ;(time (doall
    ;        (pmap #(llama-parser % {} nil) pdfs)))



    ;(wait-and-download "130bab9a-0773-4212-af32-b59de620eae2" "130bab9a-0773-4212-af32-b59de620eae2.md")
    (doseq [id ids]
      (wait-and-download id (str id ".md")))))
    ;))
