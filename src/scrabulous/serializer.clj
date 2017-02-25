(ns scrabulous.serializer
  (:require [scrabulous.board :refer [get-dim]]
            [scrabulous.game :refer [new-game new-player]]
            [scrabulous.score :refer [new-move]]
            [clojure.java.io :as io]
            [cheshire.core :refer :all]))

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
     (generate-stream (game->json game) writer))))

(defn scores->moves
  "Converts a sequence of scores to a vector of move maps"
  ([scores]
    (vec (map #(new-move [["?" %]]) scores))))

(defn json->game
  "Converts JSON to a game instance"
  ([json]
    (let [board (->> (:board json)
                  flatten
                  (map #(condp = %
                          "*" "_"
                          "" nil
                          %))
                  vec)
          players (map #(new-player [] (reduce + %) (scores->moves %)) (:moves json))]
      (new-game board players))))

(defn load-game
  "Loads game from filepath"
  ([filepath]
    (json->game (parse-string (slurp filepath) true))))
