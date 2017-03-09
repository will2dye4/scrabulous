(ns scrabulous.game
  (:require [scrabulous.board :refer :all]
            [scrabulous.score :refer [multipliers play-score]]
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

(defn new-player
  "Creates a new player map from the pre-computed components of a player"
  ([] (new-player []))
  ([tile-rack] (new-player tile-rack 0 []))
  ([tile-rack score moves]
    {:tile-rack tile-rack
     :score score
     :moves moves}))

(defn new-game
  "Creates a new game map from the pre-computed components of a game"
  ([board players] (new-game board [] multipliers players))
  ([board tile-bag multipliers players] (new-game board tile-bag {} multipliers players 1))
  ([board tile-bag blank-tiles multipliers players active-player]
   {:board board
    :tile-bag tile-bag
    :blank-tiles blank-tiles
    :multipliers multipliers
    :players (into {} (map-indexed (fn [n player] [(inc n) player]) players))
    :active active-player}))

(defn create-game
  "Creates a new game with the specified number of players,
  board size, letter frequencies, and multiplier squares"
  ([num-players] (create-game num-players dim))
  ([num-players dim] (create-game num-players dim letter-frequencies))
  ([num-players dim letter-frequencies] (create-game num-players dim letter-frequencies multipliers))
  ([num-players dim letter-frequencies multipliers]
    (let [tile-bag (create-tile-bag letter-frequencies)
          reduce-fn (fn [[bag racks] _] (let [[r b] (tile-rack bag)] [b (conj racks r)]))
          [tile-bag tile-racks] (reduce reduce-fn [tile-bag []] (range num-players))
          players (map new-player tile-racks)]
      (new-game (create-board dim) tile-bag multipliers players))))

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

(defn replace-blanks
  "Replace the blank tiles in word with the appropriate letters"
  ([game coordinates direction word]
    (let [board (:board game)
          dim (get-dim board)
          blank-tiles (:blank-tiles game)
          coordinates (word-start board coordinates direction)]
      (loop [coordinates (as-coords coordinates) original-word word replaced-word ""]
        (if (empty? original-word)
          replaced-word
          (let [replacement (blank-tiles coordinates)
                letter (or replacement (str (first original-word)))]
            (recur (next-space coordinates direction dim) (rest original-word) (str replaced-word letter))))))))

;; TODO verify blank space on either side in direction
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
        (check-spaces game coordinates direction word)
        (->> (get-cross-words (assoc game :board (place-word board coordinates direction word)) coordinates direction)
          (every? valid-word?))
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
  ([game board tile-bag tile-rack blank-locations scores]
    (-> game
      (assoc :board board)
      (assoc :tile-bag tile-bag)
      (update :blank-tiles into blank-locations)
      (assoc-in [:players (:active game) :tile-rack] tile-rack)
      (update-in [:players (:active game) :score] + (:total scores))
      (update-in [:players (:active game) :moves] conj scores)
      (assoc :active (next-player game)))))

(defn substitute-blanks
  "Returns word with replacement-tiles substituted for underscores"
  ([word replacement-tiles]
    (loop [word word tiles replacement-tiles]
      (if (empty? tiles)
        word
        (recur (string/replace-first word #"_" (first tiles)) (rest tiles))))))

(defn get-blank-locations
  "Returns a vector of tuples describing the coordinates and letter
  of the blank tiles after playing word starting at coordinates
  and moving in direction"
  ([board coordinates direction word blank-tiles]
    (let [dim (get-dim board)]
      (loop [coordinates (as-coords coordinates) word word tiles blank-tiles locations []]
        (if (empty? word)
          locations
          (let [letter (first word)
                location (when (= \_ letter) [coordinates (first tiles)])
                locations (if location (conj locations location) locations)
                tiles (if location (rest tiles) tiles)]
            (recur (next-space coordinates direction dim) (rest word) tiles locations)))))))

(declare print-state)

(defn play!
  "If the move is legal, plays word starting at coordinates
  moving in direction, updating the game state"
  ([game coordinates direction word] (play! game coordinates direction word []))
  ([game coordinates direction word blank-tiles]
    (let [coordinates (as-coords coordinates)
          board (:board @game)
          active-player (:active @game)
          player-tiles (get-in @game [:players active-player :tile-rack])
          board-tiles (get-tiles @game coordinates direction (count word))
          has-all-tiles (has-all-tiles? (concat player-tiles board-tiles) word)
          has-enough-blanks (>= (count (filter #(= \_ %) player-tiles)) (count blank-tiles))
          valid-play (valid-play? @game coordinates direction (substitute-blanks word blank-tiles))]
      (when (and has-all-tiles has-enough-blanks valid-play)
        (let [played-tiles (remove-letters board-tiles word)
              player-tiles (remove-letters played-tiles player-tiles)
              [new-tiles new-bag] (draw-tiles (:tile-bag @game) (- tiles-per-player (count player-tiles)))
              new-tiles (vec (concat player-tiles new-tiles))
              scores (play-score @game coordinates direction word (= tiles-per-player (count played-tiles)))
              blanks (get-blank-locations board coordinates direction word blank-tiles)]
          (swap! game update-game (place-word board coordinates direction word) new-bag new-tiles blanks scores))))
    (print-state @game)))

(defn pass!
  "Updates the game state to set the active player to the next player"
  ([game]
    (swap! game #(-> %
      (update-in [:players (:active %) :moves] conj {:total 0 :words [] :type :pass})
      (assoc :active (next-player %))))
    (print-state @game)))

(defn exchange!
  "Exchanges tiles for the active player and advances play to the next player"
  ([game tiles]
    (let [active-player (:active @game)
          player-tiles (get-in @game [:players active-player :tile-rack])
          has-all-tiles (has-all-tiles? player-tiles (reduce str tiles))
          enough-tiles-left (>= (count (:tile-bag @game)) (count tiles))]
      (when (and has-all-tiles enough-tiles-left)
        (let [leftover-tiles (remove-letters tiles player-tiles)
              [new-tiles new-bag] (draw-tiles (:tile-bag @game) (count tiles))
              player-tiles (concat leftover-tiles new-tiles)
              tile-bag (shuffle (concat new-bag tiles))]
          (swap! game #(-> %
            (assoc :tile-bag tile-bag)
            (assoc-in [:players active-player :tile-rack] player-tiles)
            (update-in [:players (:active %) :moves] conj {:total 0 :words [] :type :exchange})
            (assoc :active (next-player %)))))))
    (print-state @game)))

(defn print-state
  "Pretty-prints the state of game to the console"
  ([game]
    (print-board game)
    (println)
    (doseq [[n player] (:players game)]
      (println "Player" n "(" (:score player) "points )")
      (print-rack (:tile-rack player)))
    (println)
    (println (count (:tile-bag game)) "tiles remaining")
    (println "Player" (:active game) "to move")))
