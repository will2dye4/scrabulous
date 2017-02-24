(ns scrabulous.score
  (:require [scrabulous.board :refer :all]
            [clojure.string :as string]))

(def letter-values
  {0 #{\_}
   1 #{\A \E \I \O \U \L \N \R \S \T}
   2 #{\D \G}
   3 #{\B \C \M \P}
   4 #{\F \H \V \W \Y}
   5 #{\K}
   8 #{\J \X}
   10 #{\Q \Z}})

(def -multipliers
  {:double-letter [[:D 1] [:L 1] [:G 3] [:I 3] [:A 4] [:H 4] [:O 4] [:C 7]
                   [:G 7] [:I 7] [:M 7] [:D 8] [:L 8] [:C 9] [:G 9] [:I 9]
                   [:M 9] [:A 12] [:H 12] [:O 12] [:G 13] [:I 13] [:D 15] [:L 15]]
   :triple-letter [[:F 2] [:J 2] [:B 6] [:F 6] [:J 6] [:N 6]
                   [:B 10] [:F 10] [:J 10] [:N 10] [:F 14] [:J 14]]
   :double-word [[:B 2] [:N 2] [:C 3] [:M 3] [:D 4] [:L 4] [:E 5] [:K 5] [:H 8]
                 [:E 11] [:K 11] [:D 12] [:L 12] [:C 13] [:M 13] [:B 14] [:N 14]]
   :triple-word [[:A 1] [:H 1] [:O 1] [:A 8] [:O 8] [:A 15] [:H 15] [:O 15]]})

(def multipliers (into {} (map (fn [[type squares]] [type (set (map as-coords squares))]) -multipliers)))

(defn square-multipliers
  "Returns a tuple of letter and word multipliers for the square on board at coordinates"
  ([coordinates multipliers]
    (let [coordinates (as-coords coordinates)
          multiplier-entry (first (filter (fn [[type squares]] (squares coordinates)) multipliers))]
      (if multiplier-entry
        (condp = (first multiplier-entry)
          :double-letter [2 1]
          :triple-letter [3 1]
          :double-word [1 2]
          :triple-word [1 3])
        [1 1]))))

(defn letter-value
  "Returns the score for letter"
  ([letter]
    (let [letter (first (string/upper-case letter))]
      (first (first (filter (fn [[score letters]] (letters letter)) letter-values))))))

(defn word-score
  "Returns the score for word played on board
  starting at coordinates and moving in direction"
  ([game coordinates direction word]
    (let [board (:board game)
          dim (get-dim board)]
      (loop [coordinates coordinates word word word-multiplier 1 score 0]
        (if (empty? word)
          (* score word-multiplier)
          (let [letter (first word)
                value (letter-value letter)
                [letter-mult word-mult] (if (get-at board coordinates) [1 1] (square-multipliers coordinates (:multipliers game)))]
            (recur (next-space coordinates direction dim) (rest word) (* word-multiplier word-mult) (+ score (* value letter-mult)))))))))

(defn play-score
  "Returns the score for all words formed by playing word
  on board starting at coordinates and moving in direction"
  ([game coordinates direction word used-all-tiles?]
    (let [board (:board game)
          dim (get-dim board)
          opposite-direction (get-opposite direction)
          new-board (place-word board coordinates direction word)
          end (word-end new-board coordinates direction)
          bonus (if used-all-tiles? 50 0)]
      (loop [coordinates (as-coords coordinates) total (+ (word-score game coordinates direction word) bonus)]
        (let [cross-word (if (get-at board coordinates) "" (get-word (assoc game :board new-board) coordinates opposite-direction false))
              start-coords (word-start new-board coordinates opposite-direction)
              total (if (#{0 1} (count cross-word))
                      total
                      (+ total (word-score game start-coords opposite-direction cross-word)))]
          (if (= end coordinates)
            total
            (recur (next-space coordinates direction dim) total)))))))
