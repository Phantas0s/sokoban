(ns sokoban.levels
  (:require #?(:clj  [sokoban.tiles :as tiles :refer [read-tiled-map]]
               :cljs [sokoban.tiles :as tiles :refer-macros [read-tiled-map]])))

(def level-1 (read-tiled-map "1"))
(def level-2 (read-tiled-map "2"))
(def level-3 (read-tiled-map "3"))
(def level-4 (read-tiled-map "4"))
(def level-5 (read-tiled-map "5"))
(def level-6 (read-tiled-map "6"))
(def level-7 (read-tiled-map "7"))
(def level-8 (read-tiled-map "8"))
(def level-9 (read-tiled-map "9"))
(def level-10 (read-tiled-map "10"))

(def levels [level-1 level-2 level-3 level-4 level-5 level-6 level-7 level-8 level-9 level-10])
