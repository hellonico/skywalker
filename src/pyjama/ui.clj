(ns pyjama.ui
  (:require [cljfx.api :as fx]
            [clojure.java.io :as io]
            [pyjama.config]
            [pyjama.fx]
            [pyjama.io.print]
            [pyjama.io.core :as pyo]
            [pyjama.skywalker2 :refer [cag]]))

(def state (atom {:rows []}))

(defn load-files [files]
  (let [edn-file (first (seq files))
        {:keys [question-file output-file]} (pyjama.config/parse-edn-file edn-file)
        questions (with-open [reader (io/reader question-file)]
                    (doall (line-seq reader)))
        outputs (with-open [reader (io/reader output-file)]
                  (doall (line-seq reader)))

        padded-outputs (take (count questions) (concat outputs (repeat "")))
        ]
    (swap! state assoc
           :edn-file edn-file
           :processing false
           :rows (map-indexed (fn [idx [q o]] {:index idx :question q :answer o})
                              (map
                                vector
                                questions
                                padded-outputs)))))

(defmulti handle-event :event/type)
(defmethod handle-event :row-click [row-data]
  (println "Row clicked:" row-data)
  (if (true? (:processing @state))
    (println "Already in progress")
    (clojure.core.async/go
      (swap! state assoc :processing true)
      (let [

            config-file (@state :edn-file)

            _config (pyo/read-settings config-file)
            _config (assoc _config :augmented-stategy (:selection @state))

            output-file (_config :output-file)
            output-empty? (pyjama.io.core/file-empty? output-file)

            row-number (-> row-data :row :index)
            config (if output-empty?
                     (dissoc _config :replay)
                     (assoc _config :replay [row-number]))

            _ (pyjama.io.print/pretty-print-map config)
            ]
        (if output-empty?
          (swap! state assoc :row-number "all")
          (swap! state assoc :row-number row-number))
        (cag config)
        (load-files [config-file])
        (swap! state assoc :processing false))
      )))

(defn table-view [{:keys [rows processing row-number selection]}]
  {:fx/type          :stage
   :title            "Skywalker2: Replay UI"
   :on-close-request (fn [_] (System/exit 0))
   :showing          true
   :width            1200
   :height           800
   :scene            {:fx/type     :scene
                      :stylesheets #{
                                     ;                ;"extra.css"
                                     (.toExternalForm (io/resource "terminal.css"))
                                     }
                      :root        {:fx/type         :v-box
                                    :children        [{:fx/type :label
                                                       :text    "Drag and drop an EDN file here"
                                                       :padding 10}
                                                      (if (not processing)
                                                        {:fx/type          :combo-box
                                                         :items            [:sentences :full :parts]
                                                         :value            selection
                                                         :on-value-changed #(swap! state assoc :selection %)}
                                                        {:fx/type  :h-box
                                                         :children [
                                                                    {:fx/type    :image-view
                                                                     :image      (pyjama.fx/rsc-image "spinner.gif")
                                                                     :fit-width  24
                                                                     :fit-height 24}
                                                                    {:fx/type :label
                                                                     :text    (str "Replay: " row-number)
                                                                     }
                                                                    ]}
                                                        )
                                                      {:fx/type              :table-view
                                                       :v-box/vgrow          :always
                                                       :column-resize-policy :constrained
                                                       :row-factory          {:fx/cell-type :table-row
                                                                              :describe     (fn [row-data]
                                                                                              {
                                                                                               :on-mouse-clicked
                                                                                               {:event/type :row-click
                                                                                                :row        row-data}})}
                                                       :columns              [{:fx/type            :table-column
                                                                               :text               "Id"
                                                                               :pref-width         50
                                                                               :cell-value-factory :index}
                                                                              {:fx/type            :table-column
                                                                               :text               "Question"
                                                                               :pref-width         400
                                                                               :cell-value-factory :question}
                                                                              {:fx/type            :table-column
                                                                               :text               "Answer"
                                                                               :pref-width         400
                                                                               :cell-value-factory :answer}]
                                                       :items                rows
                                                       }]
                                    :on-drag-over    pyjama.fx/on-drag-over
                                    :on-drag-dropped (partial pyjama.fx/handle-files-dropped load-files)
                                    }}})
(def renderer
  (fx/create-renderer
    :middleware (fx/wrap-map-desc assoc :fx/type table-view)
    :opts {:fx.opt/map-event-handler handle-event}))

(defn -main []
  (pyjama.config/shutdown-and-startup "skywalker2" state)
  (fx/mount-renderer state renderer))
