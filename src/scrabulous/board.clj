(ns scrabulous.board
  (:require [clojure.string :as string]))

(def dim 15)

(defn create-board
  "Creates an empty board with side length dim"
  ([] (create-board dim))
  ([dim] (vec (repeat (* dim dim) nil))))

(defn get-dim
  "Returns the dimension (side length) of board"
  [board] (int (Math/sqrt (count board))))

(defn get-columns
  "Returns a vector of the column names on a board of size dim, starting at A"
  ([] (get-columns dim))
  ([dim] (mapv (comp str char) (range (int \A) (+ (int \A) dim)))))

(def columns (get-columns))

(defn column-index
  "Returns the zero-based index of the column name"
  [column] (.indexOf columns (string/upper-case (name column))))

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

(defn get-at
  "Returns the letter at coordinates on board"
  [board coordinates] (board (get-index coordinates (get-dim board))))

(defn get-letter-at
  "Returns the real letter (replacing blanks) at coordinates"
  ([game coordinates]
    (let [coordinates (as-coords coordinates)
          letter (get-at (:board game) coordinates)]
      (if (= "_" letter)
        (string/upper-case (get-in game [:blank-tiles coordinates]))
        letter))))

(defn check-spaces
  "Returns true IFF word fits into the spaces starting at coordinates and
  moving in direction. The space may be empty or have the same letter as word."
  ([game coordinates direction word]
    (if (empty? word)
      true
      (let [board (:board game)
            coordinates (as-coords coordinates)
            letter (string/upper-case (first word))
            dim (get-dim board)
            board-letter (get-letter-at game coordinates)]
        (if (or (nil? board-letter) (= letter board-letter))
          (recur game (next-space coordinates direction dim) direction (rest word))
          false)))))

(defn word-start
  "Returns the position of the start of the word
  that passes through coordinates in direction"
  ([board coordinates direction]
    (if-let [prev-coordinates (previous-space coordinates direction)]
      (if (nil? (get-at board prev-coordinates))
        coordinates
        (recur board prev-coordinates direction))
      coordinates)))

(defn word-end
  "Returns the position of the end of the word
  that passes through coordinates in direction"
  ([board coordinates direction]
    (let [dim (get-dim board)]
      (if-let [next-coordinates (next-space coordinates direction dim)]
        (if (nil? (get-at board next-coordinates))
          coordinates
          (recur board next-coordinates direction))
        coordinates))))

(defn get-word
  "Returns the word that passes through coordinates in direction"
  ([game coordinates direction replace-blanks?]
    (let [board (:board game)
          start (word-start board coordinates direction)
          end (word-end board coordinates direction)
          dim (get-dim board)]
      (loop [coords start word ""]
        (let [letter (if replace-blanks? (get-letter-at game coords) (get-at board coords)) word (str word letter)]
          (if (= coords end)
            word
            (recur (next-space coords direction dim) word)))))))

(defn get-tiles
  "Returns the tiles on the board starting at coordinates
  moving in direction for length spaces"
  ([game coordinates direction length]
    (let [dim (get-dim (:board game))]
      (loop [coordinates coordinates length length tiles []]
        (if (zero? length)
          (mapv first tiles)
          (let [tile (get-letter-at game coordinates)
                tiles (if (nil? tile) tiles (conj tiles tile))]
            (recur (next-space coordinates direction dim) (dec length) tiles)))))))

(defn get-opposite
  "Returns the opposite direction of direction (across/down)"
  [direction] (if (= direction :across) :down :across))

(defn get-cross-words
  "Returns a vector of all words crossing the word
  that passes through coordinates in direction"
  ([game coordinates direction]
    (let [board (:board game)
          start (word-start board coordinates direction)
          end (word-end board coordinates direction)
          dim (get-dim board)
          opposite-direction (get-opposite direction)]
      (loop [coords start cross-words []]
        (let [word (get-word game coords opposite-direction true)
              cross-words (if (#{0 1} (count word)) cross-words (conj cross-words word))]
          (if (= coords end)
            cross-words
            (recur (next-space coords direction dim) cross-words)))))))

(defn through-center?
  "Returns true IFF a word of the specified length starting at coordinates
  and moving in direction goes through the center of board"
  ([board coordinates direction length]
    (let [dim (get-dim board)
          center-rank (inc (quot dim 2))
          center [center-rank center-rank]]
      (loop [coordinates (as-coords coordinates) length length]
        (if (zero? length)
          false
          (if (= center coordinates)
            true
            (recur (next-space coordinates direction dim) (dec length))))))))

(defn neighbors
  "Returns a vector of spaces neighboring the space at coordinates on a board of size dim"
  ([coordinates dim]
    (->> [:across :down]
      (map #(vector (previous-space coordinates %) (next-space coordinates % dim)))
      (apply into)
      (keep identity))))

(defn connected?
  "Returns true IFF a word of the specified length starting at coordinates
  and moving in direction is connected to another word on board"
  ([board coordinates direction length]
    (let [dim (get-dim board)]
      (loop [coordinates (as-coords coordinates) length length]
        (if (zero? length)
          false
          (let [candidates (concat [coordinates] (neighbors coordinates dim))]
            (if (some #(get-at board %) candidates)
              true
              (recur (next-space coordinates direction dim) (dec length)))))))))

(defn place
  "Returns a board where letter has been placed at coordinates"
  ([board coordinates letter]
    (assoc board (get-index coordinates (get-dim board)) (string/upper-case letter))))

(defn place-word
  "Returns a board where word has been placed at coordinates in direction"
  ([board [column row :as coordinates] direction word]
    (if (empty? word)
      board
      (let [letter (first word)
            column (column-number column)
            board (if (get-at board coordinates) board (place board coordinates letter))]
        (recur board (next-space [column row] direction) direction (rest word))))))

(def terminal-colors
  {:cyan 46
   :blue 44
   :light-red 101
   :red 41
   :white 97})

(defn get-square-representation
  "Returns the representation of the square at coordinates"
  ([game coordinates letter] (get-square-representation game coordinates letter true))
  ([game coordinates letter colorize?]
    (let [center (vec (repeat 2 (inc (quot (get-dim (:board game)) 2))))
          letter (if (= "_" letter)
                   (string/lower-case (get-in game [:blank-tiles coordinates]))
                   (if (and (= center coordinates) (nil? letter)) "*" (or letter " ")))
          multiplier (when colorize? (first (filter (fn [[_ squares]] (squares coordinates)) (:multipliers game))))
          color (when multiplier
                  (condp = (first multiplier)
                    :double-letter :cyan
                    :triple-letter :blue
                    :double-word :light-red
                    :triple-word :red))
          color-str (when color (str "\u001B[1;" (terminal-colors :white) ";" (terminal-colors color) "m"))]
      (str color-str " " letter " " (when color "\u001B[0m") "|"))))

(defn print-board
  "Pretty-prints board to the console"
  ([game] (print-board game true))
  ([game colorize?]
    (let [board (:board game)
          dim (get-dim board)
          builder (StringBuilder.)
          separator (str "    +" (apply str (repeat dim "---+")) \newline)]
      (.append builder separator)
      (doseq [[row-idx row] (map-indexed vector (partition dim board))]
        (.append builder (str (if (< row-idx 9) "  " " ") (inc row-idx) " |"))
        (doseq [[col-idx square] (map-indexed vector row)]
          (.append builder (get-square-representation game [(inc col-idx) (inc row-idx)] square colorize?)))
        (.append builder \newline)
        (.append builder separator))
      (.append builder (apply str "      " (string/join "   " (get-columns dim))))
      (println (.toString builder)))))
