(ns scrabulous.core
  (:require [scrabulous.game :refer :all])
  (:gen-class))

;; TODO handle blank tiles

(def game (atom (create-game 2)))

(defn -main
  "Prints the game state to the console"
  ([& args]
    (print-state @game)))
