(ns sokoban.levels
  (:require #?(:clj  [sokoban.tiles :as tiles :refer [read-tiled-map]]
               :cljs [sokoban.tiles :as tiles :refer-macros [read-tiled-map]])))

(def level-1 (read-tiled-map "1"))
(def level-2 (read-tiled-map "2"))
(def level-3 (read-tiled-map "3"))
(def level-4 (read-tiled-map "4"))
(def level-5 (read-tiled-map "5"))

(def levels [level-1 level-2 level-3 level-4 level-5])
