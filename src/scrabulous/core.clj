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

(def board (atom (vec (repeat (* dim dim) nil))))

(def columns (vec (map (comp str char) (range 65 80))))   ;; ASCII values for 'A' - 'O'

(defn column-index [column] (.indexOf columns (string/upper-case column)))

(defn column-name [column-index] (columns column-index))

(defn next-column [column] (column-name (inc (column-index column))))

(defn get-offset [[column row]]
  (+ (* dim (dec row)) (column-index column)))

(defn letter-score [letter]
  (let [letter (if (char? letter) letter (.charAt letter 0))]
    (first (first (filter (fn [[score letters]] (letters letter)) letter-values)))))

(defn word-score [word] (reduce + (map letter-score word)))

(def tile-bag
  (-> (for [[number letters] letter-frequencies letter letters] (repeat number letter))
    vec
    flatten
    shuffle
    atom))

(defn draw-tiles [bag n]
  (let [tiles (take n @bag)]
    (swap! bag #(vec (drop n %)))
    (vec tiles)))

(defn tile-rack [bag] (draw-tiles bag tiles-per-player))

(def valid-words
  (with-open [reader (io/reader (io/resource "words.txt"))]
    (->> (line-seq reader)
      (into [])
      (keep #(re-matches #"[a-zA-Z]+" %))
      set)))

(defn place [board coordinates letter]
  (swap! board assoc (get-offset coordinates) (string/upper-case letter)))

(defn place-word [board [column row :as coordinates] direction word]
  (if (empty? word)
    nil
    (let [letter (first word)]
      (place board coordinates letter)
      (recur board (if (= direction :across) [(next-column column) row] [column (inc row)]) direction (rest word)))))

(defn print-board [board]
  (let [builder (StringBuilder.)
        separator "    +---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+\n"]
    (.append builder separator)
    (doseq [[idx row] (map-indexed vector (partition dim @board))]
      (.append builder (str (if (< idx 9) "  " " ") (inc idx) " |"))
      (doseq [square row]
        (.append builder (str " " (or square " ") " |")))
      (.append builder "\n")
      (.append builder separator))
    (.append builder "      A   B   C   D   E   F   G   H   I   J   K   L   M   N   O")
    (println (.toString builder))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
