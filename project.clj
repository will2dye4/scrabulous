(defproject scrabulous "1.0.0"
  :description "Scrabble in Clojure"
  :url "https://github.com/will2dye4/scrabulous"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [cheshire "5.7.0"]
                 ;; Java stuff
                 [org.slf4j/slf4j-log4j12 "1.7.1"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jmdk/jmxtools
                                                    com.sun.jmx/jmxri]]]
  :main ^:skip-aot scrabulous.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}}
  :resource-paths ["resources"])
