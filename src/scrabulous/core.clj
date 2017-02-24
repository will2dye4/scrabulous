(ns scrabulous.core
  (:require [scrabulous.game :refer :all])
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

(defn -main
  "Prints the game state to the console"
  ([& args]
    (print-state @game)))
