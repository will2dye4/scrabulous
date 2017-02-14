(ns scrabulous.game
  (:require [scrabulous.board :refer :all]
            [scrabulous.tiles :refer :all]
            [clojure.java.io :as io]
            [clojure.string :as string]))

(def valid-words
  (with-open [reader (io/reader (io/resource "words.txt"))]
    (->> (line-seq reader)
      (keep #(re-matches #"[a-z]+" %))
      set)))

(defn valid-word?
  "Returns true IFF word is in the dictionary"
  [word] (boolean (valid-words (string/lower-case word))))

(defn create-game
  "Creates a new game with the specified number of players,
  board size, and letter frequencies"
  ([num-players] (create-game num-players dim))
  ([num-players dim] (create-game num-players dim letter-frequencies))
  ([num-players dim letter-frequencies]
    (let [tile-bag (create-tile-bag letter-frequencies)
          reduce-fn (fn [[bag racks] _] (let [[r b] (tile-rack bag)] [b (conj racks r)]))
          [tile-bag tile-racks] (reduce reduce-fn [tile-bag []] (range num-players))
          players (into {} (map-indexed (fn [n rack] [(inc n) {:tile-rack rack :score 0}]) tile-racks))]
      {:board (create-board dim)
       :tile-bag tile-bag
       :players players
       :active 1})))

(defn get-letter-frequencies
  "Returns a map of characters to the number of occurrences of the character in word"
  ([word]
    (->> word
      (map (comp first string/upper-case))
      sort
      (partition-by identity)
      (map #(vector (first %) (count %)))
      (into {}))))

(defn has-all-tiles?
  "Returns true IFF tiles contains all letters from word"
  ([tiles word]
    (let [tile-freqs (get-letter-frequencies tiles)
          word-freqs (get-letter-frequencies word)]
      (every? #(>= (get tile-freqs % 0) (word-freqs %)) (set (string/upper-case word))))))

(defn valid-play?
  "Returns true IFF word may be played at coordinates in direction"
  ([game coordinates direction word]
    (let [board (:board game)
          [column row :as coordinates] (as-coords coordinates)
          dim (get-dim board)
          length (count word)]
      (and
        (valid-word? word)
        (<= (+ (if (= direction :across) column row) length) (inc dim))
        (check-spaces board coordinates direction word)
        (->> (get-cross-words (place-word board coordinates direction word) coordinates direction)
          (map valid-word?)
          (every? identity))
        (if (every? zero? (map (comp :score val) (:players game)))
          (through-center? board coordinates direction length)
          (connected? board coordinates direction length))))))

(defn remove-letters
  "Returns a seq of the letters in from minus the letters in to-remove"
  ([to-remove from]
    (->> (get-letter-frequencies to-remove)
      (merge-with - (get-letter-frequencies from))
      (keep (fn [[letter n]] (if (< n 1) nil (repeat n letter))))
      flatten)))

(defn next-player
  "Returns the number of the player to play next"
  ([{active-player :active players :players}]
    (if (= active-player (count players))
      1
      (inc active-player))))

(defn update-game
  "Returns updated game state after a player moves"
  ([game board tile-bag tile-rack]
    (-> game
      (assoc :board board)
      (assoc :tile-bag tile-bag)
      (assoc-in [:players (:active game) :tile-rack] tile-rack)
      (assoc :active (next-player game)))))

(declare print-state)

;; TODO handle scoring
(defn play!
  "If the move is legal, plays word starting at coordinates
  moving in direction, updating the game state"
  ([game coordinates direction word]
    (let [coordinates (as-coords coordinates)
          board (:board @game)
          active-player (:active @game)
          player-tiles (get-in @game [:players active-player :tile-rack])
          board-tiles (get-tiles board coordinates direction (count word))]
      (when (and (has-all-tiles? (concat player-tiles board-tiles) word) (valid-play? @game coordinates direction word))
        (let [played-tiles (remove-letters board-tiles word)
              player-tiles (remove-letters played-tiles player-tiles)
              [new-tiles new-bag] (draw-tiles (:tile-bag @game) (- tiles-per-player (count player-tiles)))
              new-tiles (vec (concat player-tiles new-tiles))]
          (swap! game update-game (place-word board coordinates direction word) new-bag new-tiles))))
    (print-state @game)))

(defn print-state
  "Pretty-prints the state of game to the console"
  ([game]
    (print-board (:board game))
    (println)
    (doseq [[n player] (:players game)]
      (println "Player" n "(" (:score player) "points )")
      (print-rack (:tile-rack player)))
    (println)
    (println (count (:tile-bag game)) "tiles remaining")
    (println "Player" (:active game) "to move")))
