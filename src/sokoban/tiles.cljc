(ns sokoban.tiles
  (:require #?@(:clj [[clojure.java.io :as io]
                      [tile-soup.core :as ts]])
            #?(:clj  [play-cljc.macros-java :refer [math]]
               :cljs [play-cljc.macros-js :refer-macros [math]])))

#?(:clj (defmacro read-tiled-map [fname]
          (-> (str "public/" fname)
              io/resource
              slurp
              ts/parse
              pr-str)))
