(ns scrabulous.profile
  (:require [scrabulous.serializer :refer [load-game]]
            [scrabulous.recovery :refer [recover-moves]]))

(defn recover-full-game []
  (let [input-game (load-game "resources/sample_inputs/sample_input30.json")
        recovered-games (recover-moves input-game)]
    (time (count recovered-games))))

(defn -main [& args]
  (println "Sleeping for 10 seconds...")
  (Thread/sleep 10000)
  (println "Running...")
  (recover-full-game))

(-main)
