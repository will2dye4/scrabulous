(ns scrabulous.recovery
  (:require [scrabulous.board :refer :all]
            [scrabulous.game :refer [new-game new-player remove-letters valid-words]]
            [scrabulous.score :refer [play-score]]
            [scrabulous.tiles :refer [tiles-per-player]]
            [clojure.string :as string]))

;; TODO handle wildcards (blank tiles) in word
(defn subwords
  "Returns all words that are valid subwords of word, including word itself"
  ([word]
    (if (#{0 1} (count word))
      [word]
      (filter #(.contains word %) valid-words))))

(defn containing-words
  "Returns all valid subwords of word that are larger than subword"
  ([word subword]
    (let [start-index (.indexOf word subword)
          end-index (+ start-index (dec (count subword)))]
      (keep valid-words
        (for [start-delta (range (inc start-index))
              end-delta (range end-index (count word))
              :when (not (and (= start-delta start-index) (= end-delta end-index)))]
          (subs word start-delta (inc end-delta)))))))

(defn subword-locations
  "Returns the coordinates of all occurrences of the subword
  inside the outer word starting at coordinates and moving in direction"
  ([[column row :as coordinates] direction word subword]
    (loop [word word offset 0 locations []]
      (let [index (.indexOf word subword)
            location (when (not= -1 index)
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
    (for [subword (subwords word)
          location (subword-locations coordinates direction word subword)]
      {:word subword :direction direction :coordinates location})))

(defn passes-through?
  "Returns true IFF word starting at start-coords and
  moving in direction passes through target-coords"
  ([[start-col start-row] [target-col target-row] direction word]
    (let [increment (dec (count word))
          [end-col end-row] (if (= direction :across)
                              [(+ start-col increment) start-row]
                              [start-col (+ start-row increment)])]
      (if (= direction :across)
        (and (<= start-col target-col end-col) (= start-row target-row end-row))
        (and (<= start-row target-row end-row) (= start-col target-col end-col))))))

(defn candidates-through
  "Returns a set of of candidate locations where a word
  passes through coordinates in the specified direction. If no direction
  is supplied, both directions (across and down) are considered."
  ([game coordinates] (candidates-through game coordinates nil))
  ([game coordinates direction] (candidates-through game coordinates direction true))
  ([game coordinates direction include-one-letter-words?]
    (set
      (flatten
        (for [direction (if (nil? direction) [:across :down] [direction])]
          (let [word (string/lower-case (get-word game coordinates direction false))
                start-coords (word-start (:board game) coordinates direction)]
            (->> (candidates start-coords direction word)
              (filter
                #(passes-through? (:coordinates %) coordinates direction (:word %)))
              (filter
                (if include-one-letter-words?
                  (constantly true)
                  #(> (count (:word %)) 1))))))))))

(defn touching-candidates
  "Returns a set of candidate locations where a word
  passes through coordinates in the specified direction and
  locations where a word passes through the adjacent coordinates
  in the opposite direction"
  ([game coordinates direction]
    (let [cross-candidates (candidates-through game coordinates direction false)
          board (:board game)
          dim (get-dim board)]
      (loop [candidates cross-candidates all-candidates cross-candidates]
        (if (empty? candidates)
          all-candidates
          (let [[col row :as candidate] (:coordinates (first candidates))
                opposite-direction (get-opposite direction)
                [start-col start-row :as start-coords] (word-start board candidate opposite-direction)
                adjacent? (and
                            ((neighbors coordinates dim) candidate)
                            (if (= direction :across) (= col start-col) (= row start-row)))
                all-candidates (if adjacent?
                                 (into all-candidates (candidates-through game candidate opposite-direction false))
                                 all-candidates)]
            (recur (rest candidates) all-candidates)))))))

(defn matching-moves
  "Returns a vector of possible moves (including player) given
  a game in progress, the remaining (unmatched) scores from a
  finished game, and a set of candidate words"
  ([game scores candidates]
    (loop [candidates candidates moves []]
      (if (empty? candidates)
        moves
        (let [{:keys [word direction coordinates] :as candidate} (first candidates)
              board-tiles (get-tiles game coordinates direction (count word))
              played-tiles (remove-letters board-tiles word)
              used-all? (= tiles-per-player (count played-tiles))
              score (play-score game coordinates direction word used-all?)
              players (filter (fn [[_ ss]] (= (first ss) (:total score))) scores)
              moves (apply conj moves (map #(-> candidate
                                              (assoc :player (first %))
                                              (assoc :scores score)) players))]
          (recur (rest candidates) moves))))))

(defn update-game
  "Returns updated game state after recovering a move"
  ([game {:keys [word direction coordinates player scores] :as move}]
    (-> game
      (assoc :board (place-word (:board game) coordinates direction word))
      (update-in [:players player :score] + (:total scores))
      (update-in [:players player :moves] conj scores))))

(defn cross-word-candidates
  "Returns a set of squares between start-coords and end-coords
  (inclusive) that have words crossing through them in the
  opposite of direction"
  ([game start-coords end-coords direction]
    (let [dim (get-dim (:board game))]
      (loop [coords start-coords candidates #{}]
        (let [cross-word (get-word game coords (get-opposite direction) false)
              candidates (if (#{0 1} (count cross-word)) candidates (conj candidates coords))]
          (if (= coords end-coords)
            candidates
            (recur (next-space coords direction dim) candidates)))))))

(defn recover-remaining-moves
  "Recursively tries to recover moves until
  all scores have been accounted for"
  ([finished-game test-game scores cross-candidates super-candidates]
    (if (every? empty? (vals scores))
      test-game
      (->> cross-candidates
        (map (fn [coords]
               (let [direction (if (#{0 1} (count (get-word test-game coords :across false))) :across :down)
                     candidates (touching-candidates finished-game coords direction)
                     possible-moves (matching-moves test-game scores candidates)]
                 (for [{:keys [word direction coordinates player] :as move} possible-moves]
                   (let [updated-game (update-game test-game move)
                         updated-scores (update scores player (comp vec rest))
                         end-coords (word-end (:board updated-game) coordinates direction)
                         cross-candidates (-> cross-candidates
                                            (into (cross-word-candidates finished-game coordinates end-coords direction))
                                            (disj coords))
                         full-word (get-word finished-game coordinates direction false)
                         super-candidates (if (.equalsIgnoreCase word full-word) super-candidates (conj super-candidates coordinates))]
                     (println "Recovered" move)
                     (recover-remaining-moves finished-game updated-game updated-scores cross-candidates super-candidates))))))))))

(defn recover-moves
  "Returns a vector of all possible sequences of moves that couuld have been played
  based on the final state of the board and the points scored per player per turn"
  ([game]
    (let [scores (into {} (map (fn [[n player]] [n (mapv :total (:moves player))]) (:players game)))
          dim (get-dim (:board game))
          center (vec (repeat 2 (inc (quot dim 2))))
          test-game (new-game (create-board dim) [] (:multipliers game) (repeatedly (count scores) new-player))
          possible-moves (matching-moves test-game scores (candidates-through game center))]
      (flatten
        (for [{:keys [word direction coordinates player] :as move} possible-moves]
          (let [updated-game (update-game test-game move)
                updated-scores (update scores player (comp vec rest))
                end-coords (word-end (:board updated-game) coordinates direction)
                cross-candidates (cross-word-candidates game coordinates end-coords direction)
                full-word (get-word game coordinates direction false)
                super-candidates (set (when-not (.equalsIgnoreCase word full-word) [coordinates]))]
            (println "Recovered" move)
            (recover-remaining-moves game updated-game updated-scores cross-candidates super-candidates)))))))
