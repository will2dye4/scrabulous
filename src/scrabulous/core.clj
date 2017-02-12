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

(defn create-board
  ([] (create-board dim))
  ([dim] (vec (repeat (* dim dim) nil))))

(def board (atom (create-board)))

(defn get-dim [board] (int (Math/sqrt (count board))))

(defn get-columns
  ([] (get-columns dim))
  ([dim] (vec (map (comp str char) (range (int \A) (+ (int \A) dim))))))

(def columns (get-columns))

(defn column-index [column] (.indexOf columns (string/upper-case column)))

(defn column-number [column] (if (number? column) column (inc (column-index column))))

(defn column-name [column-index] (columns column-index))

(defn next-column [column] (column-name (inc (column-index column))))

(defn next-space
  ([coordinates direction] (next-space coordinates direction dim))
  ([[column row] direction dim]
    (if (= direction :across)
      (if (= column dim)
        nil
        [(inc column) row])
      (if (= row dim)
        nil
        [column (inc row)]))))

(defn previous-space [[column row] direction]
  (if (= direction :across)
    (if (= column 1)
      nil
      [(dec column) row])
    (if (= row 1)
      nil
      [column (dec row)])))

(defn get-index
  ([coordinates] (get-index coordinates dim))
  ([[column row] dim]
    (let [column (if (number? column) (dec column) (column-index column))]
      (+ (* dim (dec row)) column))))

(defn create-tile-bag
  ([] (create-tile-bag letter-frequencies))
  ([letter-frequencies]
    (-> (for [[n letters] letter-frequencies letter letters] (repeat n letter))
      flatten
      shuffle
      vec)))

(def tile-bag (atom (create-tile-bag)))

(defn draw-tiles
  ([n] (draw-tiles tile-bag n))
  ([bag n]
    (let [tiles (take n @bag)]
      (swap! bag #(vec (drop n %)))
      (vec tiles))))

(defn tile-rack
  ([] (tile-rack tile-bag))
  ([bag] (draw-tiles bag tiles-per-player)))

(defn check-spaces [board coordinates direction word]
  (if (empty? word)
    true
    (let [letter (string/upper-case (first word))
          dim (get-dim board)
          board-letter (board (get-index coordinates dim))]
      (if (or (nil? board-letter) (= letter board-letter))
        (recur board (next-space coordinates direction dim) direction (rest word))
        false))))

(defn word-start [board coordinates direction]
  (if-let [prev-coordinates (previous-space coordinates direction)]
    (if (nil? (board (get-index prev-coordinates (get-dim board))))
      coordinates
      (recur board prev-coordinates direction))
    coordinates))

(defn word-end [board coordinates direction]
  (let [dim (get-dim board)]
    (if-let [next-coordinates (next-space coordinates direction dim)]
      (if (nil? (board (get-index next-coordinates dim)))
        coordinates
        (recur board next-coordinates direction))
      coordinates)))

(defn get-word [board coordinates direction]
  (let [start (word-start board coordinates direction)
        end (word-end board coordinates direction)
        dim (get-dim board)]
    (loop [coords start word ""]
      (let [letter (board (get-index coords dim)) word (str word letter)]
        (if (= coords end)
          word
          (recur (next-space coords direction dim) word))))))

(defn get-minor-words [board [column row :as coordinates] direction]
  (let [start (word-start board coordinates direction)
        end (word-end board coordinates direction)
        dim (get-dim board)
        opposite-direction (if (= direction :across) :down :across)]
    (loop [coords start minor-words []]
      (let [minor-word (get-word board coords opposite-direction)
            minor-words (if (#{0 1} (count minor-word)) minor-words (conj minor-words minor-word))]
        (if (= coords end)
          minor-words
          (recur (next-space coords direction dim) minor-words))))))

(declare place-word)

(defn valid-play?
  ([coordinates direction word] (valid-play? @board coordinates direction word))
  ([board [column row :as coordinates] direction word]
    (let [column (column-number column) dim (get-dim board)]
      (and
        (valid-words (string/lower-case word))
        (<= (+ (if (= direction :across) column row) (count word)) (inc dim))
        (check-spaces board [column row] direction word)
        (->> (get-minor-words (place-word board coordinates direction word) [column row] direction)
          (map (comp valid-words string/lower-case))
          (every? boolean))))))

(defn place
  ([coordinates letter] (place @board coordinates letter))
  ([board coordinates letter]
    (assoc board (get-index coordinates (get-dim board)) (string/upper-case letter))))

(defn place!
  ([coordinates letter] (place! board coordinates letter))
  ([board coordinates letter]
    (swap! board place coordinates letter)))

(defn place-word
  ([coordinates direction word] (place-word @board coordinates direction word)) 
  ([board [column row :as coordinates] direction word]
    (if (empty? word)
      board
      (let [letter (first word)
            column (column-number column)
            board (place board coordinates letter)]
        (recur board (next-space [column row] direction) direction (rest word))))))

(defn place-word!
  ([coordinates direction word] (place-word! board coordinates direction word)) 
  ([board coordinates direction word]
    (swap! board place-word coordinates direction word)))

(defn letter-score [letter]
  (let [letter (if (char? letter) letter (first (string/upper-case letter)))]
    (first (first (filter (fn [[score letters]] (letters letter)) letter-values)))))

(defn word-score [word] (reduce + (map letter-score word)))

(defn print-board
  ([] (print-board @board))
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
  ([rack] (print-rack rack true))
  ([rack sort?]
    (println (str "[" (string/join " " (if sort? (sort rack) rack)) "]"))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
