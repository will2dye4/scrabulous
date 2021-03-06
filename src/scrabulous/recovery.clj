(ns scrabulous.recovery
  (:require [scrabulous.board :refer :all]
            [scrabulous.game :refer [new-game new-player remove-letters valid-words valid-word?]]
            [scrabulous.score :refer [play-score]]
            [scrabulous.tiles :refer [tiles-per-player]]
            [clojure.string :as string]
            [clojure.tools.logging :as log]))

(defn subwords
  "Returns all words that are valid subwords of word, including word itself"
  ([word] (subwords word (.contains word "_")))
  ([word include-invalid?]
    (if (#{0 1} (count word))
      [word]
      (->>
        (for [start-index (range (count word))
              end-index (range (inc start-index) (inc (count word)))]
          (subs word start-index end-index))
        (filter
          (if include-invalid?
            (constantly true)
            valid-word?))
        set))))

;; TODO handle blank tiles
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
    (if (empty? word)
      []
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
              (recur (subs word skip) (+ offset skip) locations))))))))

(defn containing-word-location
  "Returns the coordinates of the occurrence of word
  in the word starting at coordinates and moving in direction"
  ([game coordinates direction word]
    (let [[start-col start-row] (word-start (:board game) coordinates direction)
          full-word (string/lower-case (get-word game coordinates direction false))
          index (.indexOf full-word word)]
      (when (not= -1 index)
        (if (= direction :across)
          [(+ start-col index) start-row]
          [start-col (+ start-row index)])))))

(defn candidates
  "Returns a sequence of maps describing all locations where a subword
  occurs in the outer word starting at coordinates and moving in direction"
  ([coordinates direction word]
    (for [subword (subwords word)
          location (subword-locations coordinates direction word subword)]
      {:word subword :direction direction :coordinates location})))

(defn containing-word-candidates
  "Returns a sequence of maps describing all locations where a containing word
  occurs in the word starting at coordinates and moving in direction"
  ([game coordinates direction word subword]
   (->>
     (for [containing-word (containing-words word subword)]
       (let [location (containing-word-location game coordinates direction containing-word)]
         (when (not (nil? location))
           {:word containing-word :direction direction :coordinates location})))
     (keep identity))))

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
    (apply concat
      (candidates-through game coordinates direction true)
      (->> #{(previous-space coordinates direction) (next-space coordinates direction (get-dim (:board game)))}
        (keep identity)
        (map #(candidates-through game % (get-opposite direction) false))))))

;; ex. - AMPLE was played horizontally
;;     - possible containing words: EXAMPLE, EXAMPLES
;;     - possible opposite-direction words: AXE, ASK (but AXE would create a non-word XAMPLE horizontally)
;;  A     A
;; EXAMPLES
;;  E     K
(defn containing-candidates
  "Returns a set of candidate locations where a containing word
  passes through coordinates in the specified direction"
  ([finished-game test-game coordinates direction]
    (let [current-word (string/lower-case (get-word test-game coordinates direction false))
          full-word (string/lower-case (get-word finished-game coordinates direction false))]
      (apply concat
        (containing-word-candidates finished-game coordinates direction full-word current-word)
        (let [start-coords (word-start (:board test-game) coordinates direction)
              end-coords (word-end (:board test-game) coordinates direction)
              previous-coords (previous-space start-coords direction)
              next-coords (next-space end-coords direction (get-dim (:board finished-game)))]
          (->> #{previous-coords next-coords}
            (keep identity)
            (map #(candidates-through finished-game % (get-opposite direction) false))))))))

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
              score (play-score game coordinates direction (string/upper-case word) used-all?)
              players (filter (fn [[_ ss]] (= (first ss) (:total score))) scores)
              moves (apply conj moves (map #(-> candidate
                                              (assoc :player (first %))
                                              (assoc :scores score)) players))]
          (recur (rest candidates) moves))))))

(defn matching-moves-alternating
  "Returns a vector of possible moves for the currently active player
  given a game in progress, the remaining (unmatched) scores from a
  finished game, and a set of candidate words"
  ([game scores candidates]
    (loop [candidates candidates moves []]
      (if (empty? candidates)
        moves
        (let [{:keys [word direction coordinates] :as candidate} (first candidates)
              board-tiles (get-tiles game coordinates direction (count word))
              played-tiles (remove-letters board-tiles word)
              used-all? (= tiles-per-player (count played-tiles))
              score (play-score game coordinates direction (string/upper-case word) used-all?)
              moves (if (= (first (scores (:active game))) (:total score))
                      (conj moves
                        (-> candidate
                          (assoc :player (:active game))
                          (assoc :scores score)))
                      moves)]
          (recur (rest candidates) moves))))))

(defn remove-first-score
  "Returns an updated score map with the first score for player removed"
  ([scores player]
    (update scores player (comp vec rest))))

(defn next-player
  "Returns the number of the player to play next"
  ([active-player scores]
    (if (every? empty? (vals scores))
      active-player
      (let [num-players (count scores)
            next-player (if (= active-player num-players) 1 (inc active-player))]
        (if-not (zero? (or (first (scores next-player)) 0))
          next-player
          (recur next-player (remove-first-score scores next-player)))))))

(defn update-game
  "Returns updated game state after recovering a move"
  ([game {:keys [word direction coordinates player scores] :as move} player-scores]
    (-> game
      (assoc :board (place-word (:board game) coordinates direction word))
      (update-in [:players player :score] + (:total scores))
      (update-in [:players player :moves] conj scores)
      (assoc :active (next-player player (remove-first-score player-scores player))))))

(defn update-scores
  "Returns an updated score map, eliminating zero scores as necessary"
  ([scores active-player]
    (if (every? empty? (vals scores))
      scores
      (let [scores (remove-first-score scores active-player)
            next-player (if (= active-player (count scores)) 1 (inc active-player))]
        (if-not (zero? (or (first (scores next-player)) 0))
          scores
          (recur scores next-player))))))

(defn cross-word-candidates
  "Returns a set of squares between start-coords and end-coords
  (inclusive) that have words crossing through them in the
  opposite of direction"
  ([finished-game test-game start-coords end-coords direction]
    (let [dim (get-dim (:board finished-game))]
      (loop [coords start-coords candidates #{}]
        (let [full-cross-word (get-word finished-game coords (get-opposite direction) false)
              current-cross-word (get-word test-game coords (get-opposite direction) false)
              candidates (if (or (#{0 1} (count full-cross-word)) (> (count current-cross-word) 1))
                           candidates
                           (conj candidates coords))]
          (if (= coords end-coords)
            candidates
            (recur (next-space coords direction dim) candidates)))))))

(declare recover-remaining-moves)

(defn recover-from-moves
  "Recovers remaining moves for each starting move"
  ([possible-moves target-coords finished-game test-game scores cross-candidates
    super-candidates update-cross-candidates update-super-candidates]
    (for [{:keys [word direction coordinates player] :as move} possible-moves]
      (let [updated-game (update-game test-game move scores)
            updated-scores (update-scores scores player)
            end-coords (word-end (:board updated-game) coordinates direction)
            cross-candidates (update-cross-candidates cross-candidates finished-game updated-game coordinates end-coords direction target-coords)
            full-word (get-word finished-game coordinates direction false)
            super-candidates (update-super-candidates super-candidates word full-word coordinates target-coords)]
        (log/debug "Recovered move" move "from candidate" target-coords)
        (recover-remaining-moves finished-game updated-game updated-scores cross-candidates super-candidates)))))

(defn update-cross-candidates-for-cross-word
  "Returns an updated set of cross-word candidates after playing a cross-word move"
  ([cross-candidates finished-game test-game coordinates end-coords direction target-coords]
   (-> cross-candidates
     (into (cross-word-candidates finished-game test-game coordinates end-coords direction))
     (disj target-coords))))

(defn update-super-candidates-for-cross-word
  "Returns an updated set of containing-word candidates after playing a cross-word move"
  ([super-candidates word full-word coordinates target-coords]
   (if (.equalsIgnoreCase word full-word) super-candidates (conj super-candidates coordinates))))

(defn update-cross-candidates-for-containing-word
  "Returns an updated set of cross-word candidates after playing a containing-word move"
  ([cross-candidates finished-game test-game coordinates end-coords direction target-coords]
   (into cross-candidates (cross-word-candidates finished-game test-game coordinates end-coords direction))))

(defn update-super-candidates-for-containing-word
  "Returns an updated set of containing-word candidates after playing a containing-word move"
  ([super-candidates word full-word coordinates target-coords]
   (let [super-candidates (disj super-candidates target-coords)]
     (if (.equalsIgnoreCase word full-word) super-candidates (conj super-candidates target-coords)))))

(defn recover-remaining-moves
  "Recursively tries to recover moves until
  all scores have been accounted for"
  ([finished-game test-game scores cross-candidates super-candidates]
    (if (every? empty? (vals scores))
      (if (= (:board finished-game) (:board test-game))
        (log/spyf "---- Finished with game ----" test-game)
        [])
      (concat
        (for [coords cross-candidates]
          (let [direction (if (#{0 1} (count (get-word test-game coords :across false))) :across :down)
                candidates (touching-candidates finished-game coords direction)
                possible-moves (matching-moves-alternating test-game scores candidates)]
            (recover-from-moves possible-moves coords finished-game test-game scores cross-candidates super-candidates
              update-cross-candidates-for-cross-word update-super-candidates-for-cross-word)))
        (for [coords super-candidates
              direction [:across :down]
              :when (not= (get-word finished-game coords direction false) (get-word test-game coords direction false))]
          (let [candidates (set (containing-candidates finished-game test-game coords direction))
                possible-moves (matching-moves-alternating test-game scores candidates)]
            (recover-from-moves possible-moves coords finished-game test-game scores cross-candidates super-candidates
              update-cross-candidates-for-containing-word update-super-candidates-for-containing-word)))))))

(defn initial-cross-candidates
  "Returns the initial set of cross-word candidates after playing a move"
  ([cross-candidates finished-game test-game coordinates end-coords direction target-coords]
   (cross-word-candidates finished-game test-game coordinates end-coords direction)))

(defn initial-super-candidates
  "Returns the initial set of containing-word candidates after playing a move"
  ([super-candidates word full-word coordinates target-coords]
   (set (when-not (.equalsIgnoreCase word full-word) [coordinates]))))

(defn recover-moves
  "Returns a vector of all possible sequences of moves that couuld have been played
  based on the final state of the board and the points scored per player per turn"
  ([game]
    (let [scores (into {} (map (fn [[n player]] [n (mapv :total (:moves player))]) (:players game)))
          dim (get-dim (:board game))
          center (vec (repeat 2 (inc (quot dim 2))))
          test-game (new-game (create-board dim) [] (:multipliers game) (repeatedly (count scores) new-player))
          possible-moves (matching-moves test-game scores (candidates-through game center))]
      (distinct
        (flatten
          (recover-from-moves possible-moves center game test-game scores #{} #{} initial-cross-candidates initial-super-candidates))))))
