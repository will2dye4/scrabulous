(ns scrabulous.serializer
  (:require [scrabulous.board :refer [get-dim]]
            [clojure.java.io :as io]
            [cheshire.core :as json]))

(defn game->json
  "Converts game to the expected JSON format"
  ([game]
    (let [board (:board game)
          dim (get-dim board)
          new-board (->> board
                      (map #(if (= "_" %) "*" (or % "")))
                      (partition dim)
                      (map vec)
                      (into []))
          moves (vec (map (fn [[_ player]] (vec (map :total (:moves player)))) (:players game)))]
      {:board new-board
       :moves moves})))

(defn save-game
  "Saves game to filepath"
  ([game filepath]
   (with-open [writer (io/writer filepath)]
     (json/generate-stream (game->json game) writer))))
