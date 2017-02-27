(ns scrabulous.recovery
  (:require [scrabulous.board :refer :all]
            [scrabulous.game :refer [valid-words]]))

(defn subwords
  "Returns all words that are valid subwords of word, including word itself"
  ([word]
   (if (#{0 1} (count word))
     [word]
     (filter #(.contains word %) valid-words))))

(defn subword-locations
  "Returns the coordinates of all occurrences of the subword
  inside the outer word starting at coordinates and moving in direction"
  ([[column row :as coordinates] direction word subword]
    (loop [word word offset 0 locations []]
      (let [index (.indexOf word subword)
            location (when (not (= -1 index))
                       (if (= direction :across)
                         [(+ column index offset) row]
                         [column (+ row index offset)]))
            locations (if location (conj locations location) locations)]
        (if (= -1 index)
          locations
          (let [skip (+ index (count subword))]
            (recur (subs word skip) (+ offset skip) locations)))))))

(defn candidates
  "Returns a map of subwords and all locations where the subword
  occurs in the outer word starting at coordinates and moving in direction"
  ([coordinates direction word]
    (->> (subwords word)
      (map #(vector % (subword-locations coordinates direction word %)))
      (into {}))))

(defn passes-through?
  "Returns true IFF word starting at start-coords and
  moving in direction passes through target-coords"
  ([start-coords target-coords direction word]
   (let [increment (dec (count word))
         [start-col start-row] start-coords
         [target-col target-row] target-coords
         [end-col end-row] (if (= direction :across)
                             [(+ start-col increment) start-row]
                             [start-col (+ start-row increment)])]
     (if (= direction :across)
       (and (<= start-col target-col end-col) (= start-row target-row end-row))
       (and (<= start-row target-row end-row) (= start-col target-col end-col))))))

(defn candidates-through
  "Returns a map of subwords and all locations where the subword
  occurs in the outer word starting at start-coords and moving in direction
  AND passes through through-coords"
  ([start-coords target-coords direction word]
    (->> (candidates start-coords direction word)
      (map (fn [[subword locations]]
             [subword (vec (filter #(passes-through? % target-coords direction subword) locations))]))
      (filter #(not (empty? (second %))))
      (into {}))))

(defn recover-moves
  "Returns a vector of all possible sequences of moves that couuld have been played
  based on the final state of the board and the points scored per player per turn"
  ([game]
    (let [scores (into {} (map (fn [[n player]] [n (mapv :total (:moves player))]) (:players game)))
          final-board (:board game)
          dim (get-dim final-board)
          new-board (create-board dim)
          center (vec (repeat 2 (inc (quot dim 2))))
          center-words (map #(get-word game center % false) [:across :down])])))
