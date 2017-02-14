(ns scrabulous.tiles
  (:require [clojure.string :as string]))

(def tiles-per-player 7)

(def letter-frequencies
  {1 #{\J \K \Q \X \Z}
   2 #{\B \C \F \H \M \P \V \W \Y}
   3 #{\G}
   4 #{\D \L \S \U}
   6 #{\N \R \T}
   8 #{\O}
   9 #{\A \I}
   12 #{\E}})

(defn create-tile-bag
  "Creates a tile bag with the specified letter frequencies"
  ([] (create-tile-bag letter-frequencies))
  ([letter-frequencies]
    (-> (for [[n letters] letter-frequencies letter letters] (repeat n letter))
      flatten
      shuffle
      vec)))

(defn draw-tiles
  "Draws n tiles from the tile bag, returning a tuple
  containing the tiles drawn and the bag with the tiles removed"
  [tile-bag n] [(vec (take n tile-bag)) (vec (drop n tile-bag))])

(defn tile-rack
  "Draws the default number of tiles from the tile bag,
  returning the same as draw-tiles"
  [tile-bag] (draw-tiles tile-bag tiles-per-player))

(defn print-rack
  "Pretty-prints (optionally sorted) rack to the console"
  ([rack] (print-rack rack true))
  ([rack sort?]
    (println (str "[" (string/join " " (if sort? (sort rack) rack)) "]"))))
