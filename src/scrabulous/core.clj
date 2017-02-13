(ns scrabulous.core
  (:require [clojure.java.io :as io]
            [clojure.string :as string])
  (:gen-class))

(def dim 15)

(def tiles-per-player 7)

(def letter-values
  {1 #{\A \E \I \O \U \L \N \R \S \T}
   2 #{\D \G}
   3 #{\B \C \M \P}
   4 #{\F \H \V \W \Y}
   5 #{\K}
   8 #{\J \X}
   10 #{\Q \Z}})

(def letter-frequencies
  {1 #{\J \K \Q \X \Z}
   2 #{\B \C \F \H \M \P \V \W \Y}
   3 #{\G}
   4 #{\D \L \S \U}
   6 #{\N \R \T}
   8 #{\O}
   9 #{\A \I}
   12 #{\E}})

(def valid-words
  (with-open [reader (io/reader (io/resource "words.txt"))]
    (->> (line-seq reader)
      (keep #(re-matches #"[a-z]+" %))
      set)))

(defn valid-word?
  "Returns true IFF word is in the dictionary"
  [word] (boolean (valid-words (string/lower-case word))))

(defn get-dim
  "Returns the dimension (side length) of board"
  [board] (int (Math/sqrt (count board))))

(defn get-columns
  "Returns a vector of the column names on a board of size dim, starting at A"
  ([] (get-columns dim))
  ([dim] (vec (map (comp str char) (range (int \A) (+ (int \A) dim))))))

(def columns (get-columns))

(defn column-index
  "Returns the zero-based index of the column name"
  [column] (.indexOf columns (string/upper-case column)))

(defn column-number
  "Returns the one-based index of the column"
  [column] (if (number? column) column (inc (column-index column))))

(defn as-coords
  "Returns the one-based numeric coordinates for column and row"
  [[column row]] [(column-number column) row])

(defn column-name
  "Returns the name of the zero-based column-index"
  [column-index] (columns column-index))

(defn next-column
  "Returns the name of the next column after column"
  [column] (column-name (inc (column-index column))))

(defn next-space
  "Returns the next space in the specified direction,
  or nil if the space is on the edge"
  ([coordinates direction] (next-space coordinates direction dim))
  ([coordinates direction dim]
    (let [[column row] (as-coords coordinates)]
      (if (= direction :across)
        (if (= column dim)
          nil
          [(inc column) row])
        (if (= row dim)
          nil
          [column (inc row)])))))

(defn previous-space
  "Returns the previous space in the specified direction,
  or nil if the space is on the edge"
  ([coordinates direction]
    (let [[column row] (as-coords coordinates)]
      (if (= direction :across)
        (if (= column 1)
          nil
          [(dec column) row])
        (if (= row 1)
          nil
          [column (dec row)])))))

(defn get-index
  "Returns the zero-based index of the square on a board of size dim"
  ([coordinates] (get-index coordinates dim))
  ([[column row] dim]
    (let [column (if (number? column) (dec column) (column-index column))]
      (+ (* dim (dec row)) column))))

(defn create-board
  "Creates an empty board with side length dim"
  ([] (create-board dim))
  ([dim] (vec (repeat (* dim dim) nil))))

(defn create-tile-bag
  "Creates a tile bag with the specified letter frequencies"
  ([] (create-tile-bag letter-frequencies))
  ([letter-frequencies]
    (-> (for [[n letters] letter-frequencies letter letters] (repeat n letter))
      flatten
      shuffle
      vec)))

;; TODO handle case where tile-bag has fewer than n tiles
(defn draw-tiles
  "Draws n tiles from the tile bag, returning a tuple
  containing the tiles drawn and the bag with the tiles removed"
  [tile-bag n] [(vec (take n tile-bag)) (vec (drop n tile-bag))])

(defn tile-rack
  "Draws the default number of tiles from the tile bag,
  returning the same as draw-tiles"
  [tile-bag] (draw-tiles tile-bag tiles-per-player))

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

(def game (atom (create-game 2)))

(defn check-spaces
  "Returns true IFF word fits into the spaces starting at coordinates and
  moving in direction. The space may be empty or have the same letter as word."
  ([board coordinates direction word]
    (if (empty? word)
      true
      (let [letter (string/upper-case (first word))
            dim (get-dim board)
            board-letter (board (get-index coordinates dim))]
        (if (or (nil? board-letter) (= letter board-letter))
          (recur board (next-space coordinates direction dim) direction (rest word))
          false)))))

(defn word-start
  "Returns the position of the start of the word
  that passes through coordinates in direction"
  ([board coordinates direction]
    (if-let [prev-coordinates (previous-space coordinates direction)]
      (if (nil? (board (get-index prev-coordinates (get-dim board))))
        coordinates
        (recur board prev-coordinates direction))
      coordinates)))

(defn word-end
  "Returns the position of the end of the word
  that passes through coordinates in direction"
  ([board coordinates direction]
    (let [dim (get-dim board)]
      (if-let [next-coordinates (next-space coordinates direction dim)]
        (if (nil? (board (get-index next-coordinates dim)))
          coordinates
          (recur board next-coordinates direction))
        coordinates))))

(defn get-word
  "Returns the word that passes through coordinates in direction"
  ([board coordinates direction]
    (let [start (word-start board coordinates direction)
          end (word-end board coordinates direction)
          dim (get-dim board)]
      (loop [coords start word ""]
        (let [letter (board (get-index coords dim)) word (str word letter)]
          (if (= coords end)
            word
            (recur (next-space coords direction dim) word)))))))

(defn get-tiles
  "Returns the tiles on the board starting at coordinates
  moving in direction for length spaces"
  ([board coordinates direction length]
    (let [dim (get-dim board)]
      (loop [coordinates coordinates length length tiles []]
        (if (zero? length)
          (vec (map first tiles))
          (let [tile (board (get-index coordinates))
                tiles (if (nil? tile) tiles (conj tiles tile))]
            (recur (next-space coordinates direction dim) (dec length) tiles)))))))

(defn get-cross-words
  "Returns a vector of all words crossing the word
  that passes through coordinates in direction"
  ([board coordinates direction]
    (let [start (word-start board coordinates direction)
          end (word-end board coordinates direction)
          dim (get-dim board)
          opposite-direction (if (= direction :across) :down :across)]
      (loop [coords start cross-words []]
        (let [word (get-word board coords opposite-direction)
              cross-words (if (#{0 1} (count word)) cross-words (conj cross-words word))]
          (if (= coords end)
            cross-words
            (recur (next-space coords direction dim) cross-words)))))))

(declare place-word)

;; TODO play must be connected to another word
(defn valid-play?
  "Returns true IFF word may be played at coordinates in direction"
  ([coordinates direction word] (valid-play? (:board @game) coordinates direction word))
  ([board coordinates direction word]
    (let [[column row :as coordinates] (as-coords coordinates) dim (get-dim board)]
      (and
        (valid-word? word)
        (<= (+ (if (= direction :across) column row) (count word)) (inc dim))
        (check-spaces board coordinates direction word)
        (->> (get-cross-words (place-word board coordinates direction word) coordinates direction)
          (map valid-word?)
          (every? identity))))))

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

(defn place
  "Returns a board where letter has been placed at coordinates"
  ([coordinates letter] (place (:board @game) coordinates letter))
  ([board coordinates letter]
    (assoc board (get-index coordinates (get-dim board)) (string/upper-case letter))))

(defn place-word
  "Returns a board where word has been placed at coordinates in direction"
  ([coordinates direction word] (place-word (:board @game) coordinates direction word)) 
  ([board [column row :as coordinates] direction word]
    (if (empty? word)
      board
      (let [letter (first word)
            column (column-number column)
            board (place board coordinates letter)]
        (recur board (next-space [column row] direction) direction (rest word))))))

(defn place-word!
  "Swaps the game's board, placing word at coordinates in direction"
  ([coordinates direction word] (place-word! game coordinates direction word)) 
  ([game coordinates direction word]
    (swap! game assoc :board (place-word (:board @game) coordinates direction word))))

(defn letter-score
  "Returns the score for letter"
  ([letter]
    (let [letter (if (char? letter) letter (first (string/upper-case letter)))]
      (first (first (filter (fn [[score letters]] (letters letter)) letter-values))))))

(defn word-score
  "Returns the score for word (sum of letter scores)"
  [word] (reduce + (map letter-score word)))

(defn print-board
  "Pretty-prints board to the console"
  ([] (print-board (:board @game)))
  ([board]
    (let [dim (get-dim board)
          builder (StringBuilder.)
          separator (str "    +" (apply str (repeat dim "---+")) \newline)]
      (.append builder separator)
      (doseq [[idx row] (map-indexed vector (partition dim board))]
        (.append builder (str (if (< idx 9) "  " " ") (inc idx) " |"))
        (doseq [square row]
          (.append builder (str " " (or square " ") " |")))
        (.append builder \newline)
        (.append builder separator))
      (.append builder (apply str "      " (string/join "   " (get-columns dim))))
      (println (.toString builder)))))

(defn print-rack
  "Pretty-prints (optionally sorted) rack to the console"
  ([rack] (print-rack rack true))
  ([rack sort?]
    (println (str "[" (string/join " " (if sort? (sort rack) rack)) "]"))))

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

(defn remove-letters
  "Returns a seq of the letters in from minus the letters in to-remove"
  ([to-remove from]
    (->> (get-letter-frequencies to-remove)
      (merge-with - (get-letter-frequencies from))
      (keep (fn [[letter n]] (if (< n 1) nil (repeat n letter))))
      flatten)))

;; TODO handle scoring    
(defn play [game coordinates direction word]
  (let [coordinates (as-coords coordinates)
        board (:board @game)
        active-player (:active @game)
        player-tiles (get-in @game [:players active-player :tile-rack])
        board-tiles (get-tiles board coordinates direction (count word))]
    (when (and (has-all-tiles? (into player-tiles board-tiles) word) (valid-play? board coordinates direction word))
      (let [played-tiles (remove-letters board-tiles word)
            player-tiles (remove-letters played-tiles player-tiles)
            [new-tiles new-bag] (draw-tiles (:tile-bag @game) (- tiles-per-player (count player-tiles)))
            next-player (if (= active-player (count (:players @game))) 1 (inc active-player))]
        (place-word! game coordinates direction word)
        (swap! game assoc :tile-bag new-bag)
        (swap! game assoc-in [:players active-player :tile-rack] (vec (into player-tiles new-tiles)))
        (swap! game assoc :active next-player))))
  (print-state @game))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
