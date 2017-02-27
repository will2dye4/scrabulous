(ns scrabulous.recovery
  (:require [scrabulous.board :refer :all]
            [scrabulous.game :refer [valid-words]]))

(defn subwords
  "Returns all words that are valid subwords of word, including word itself"
  [word] (filter #(.contains word %) valid-words))

(defn recover-moves
  "Returns a vector of all possible sequences of moves that couuld have been played
  based on the final state of the board and the points scored per player per turn"
  ([game]
    (let [scores (into {} (map (fn [[n player]] [n (mapv :total (:moves player))]) (:players game)))
          final-board (:board game)
          dim (get-dim final-board)
          new-board (create-board dim)
          center (vec (repeat 2 (inc (quot dim 2))))
          center-moves (map #(get-word game center % false) [:across :down])])))
