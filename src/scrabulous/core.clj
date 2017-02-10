(ns scrabulous.core
  (:require [clojure.string :as string])
  (:gen-class))

(def dim 15)

(def board (atom (vec (repeat (* dim dim) nil))))

(def columns (vec (map (comp str char) (range 65 80))))

(def letter-values
  {1 #{\A \E \I \O \U \L \N \R \S \T}
   2 #{\D \G}
   3 #{\B \C \M \P}
   4 #{\F \H \V \W \Y}
   5 #{\K}
   8 #{\J \X}
   10 #{\Q \Z}})

(defn letter-score [letter]
  (let [letter (if (char? letter) letter (.charAt letter 0))]
    (first (first (filter (fn [[score letters]] (letters letter)) letter-values)))))

(defn column-index [column] (.indexOf columns (string/upper-case column)))

(defn column-name [column-index] (columns column-index))

(defn next-column [column] (column-name (inc (column-index column))))

(defn get-offset [[column row]]
  (+ (* dim (column-index column)) (dec row)))

(defn place [board coordinates letter]
  (swap! board assoc (get-offset coordinates) (string/upper-case letter)))

(defn place-word [board [column row :as coordinates] direction word]
  (if (empty? word)
    nil
    (let [letter (first word)]
      (place board coordinates letter)
      (recur board (if (= direction :across) [column (inc row)] [(next-column column) row]) direction (rest word)))))

(defn print-board [board]
  (let [builder (StringBuilder.)
        separator "+---+---+---+---+---+---+---+---+---+---+---+---+---+---+---+\n"]
    (.append builder separator)
    (doseq [row (partition dim @board)]
      (.append builder "|")
      (doseq [square row]
        (.append builder (str " " (or square " ") " |")))
      (.append builder "\n")
      (.append builder separator))
    (println (.toString builder))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
