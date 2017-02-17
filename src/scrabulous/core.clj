(ns scrabulous.core
  (:require [scrabulous.game :refer :all])
  (:gen-class))

(def game (atom (create-game 2)))

(defn play
  ([coordinates direction word] (play coordinates direction word []))
  ([coordinates direction word blank-tiles] (play! game coordinates direction word blank-tiles)))

(defn pass [] (pass! game))

(defn exchange [tiles] (exchange! game tiles))

(defn -main
  "Prints the game state to the console"
  ([& args]
    (print-state @game)))
