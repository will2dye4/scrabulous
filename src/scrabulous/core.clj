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
      (keep #(re-matches #"[a-zA-Z]+" %))
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

(defn column-name [column-index] (columns column-index))

(defn next-column [column] (column-name (inc (column-index column))))

(defn get-index
  ([coordinates] (get-index coordinates dim))
  ([[column row] dim]
    (let [column (if (number? column) (dec column) (column-index column))]
      (+ (* dim (dec row)) column))))

(defn create-tile-bag
  ([] (create-tile-bag letter-frequencies))
  ([letter-frequencies]
    (-> (for [[number letters] letter-frequencies letter letters] (repeat number letter))
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

(defn place
  ([coordinates letter] (place board coordinates letter))
  ([board coordinates letter]
    (swap! board assoc (get-index coordinates (get-dim @board)) (string/upper-case letter))))

(defn place-word
  ([coordinates direction word] (place-word board coordinates direction word)) 
  ([board [column row :as coordinates] direction word]
    (if (empty? word)
      @board
      (let [letter (first word) column (if (number? column) column (inc (column-index column)))]
        (place board coordinates letter)
        (recur board (if (= direction :across) [(inc column) row] [column (inc row)]) direction (rest word))))))

(defn letter-score [letter]
  (let [letter (if (char? letter) letter (.charAt letter 0))]
    (first (first (filter (fn [[score letters]] (letters letter)) letter-values)))))

(defn word-score [word] (reduce + (map letter-score word)))

(defn print-board
  ([] (print-board board))
  ([board]
    (let [dim (get-dim @board)
          builder (StringBuilder.)
          separator (str "    +" (apply str (repeat dim "---+")) \newline)]
      (.append builder separator)
      (doseq [[idx row] (map-indexed vector (partition dim @board))]
        (.append builder (str (if (< idx 9) "  " " ") (inc idx) " |"))
        (doseq [square row]
          (.append builder (str " " (or square " ") " |")))
        (.append builder "\n")
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
