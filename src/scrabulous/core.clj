(ns scrabulous.core
  (:require [scrabulous.game :refer :all]
            [scrabulous.recovery :refer [recover-moves]]
            [scrabulous.serializer :refer [load-game]])
  (:gen-class))

(def game (atom (create-game 2)))

(defmacro play
  ([coordinates direction word] (list `play coordinates direction word []))
  ([coordinates direction word blank-tiles]
    (let [coords# (name coordinates)
          col# (subs coords# 0 1)
          row# (Integer/parseInt (subs coords# 1))
          dir# (keyword direction)
          word# (name word)]
      (list `play! `game [col# row#] dir# word# blank-tiles))))

(defn pass [] (pass! game))

(defn exchange [tiles] (exchange! game tiles))

(defn test-recovery [] (recover-moves (load-game "/tmp/partial_game.json")))

(defn -main
  "Prints the game state to the console"
  ([& args]
    (print-state @game)))
