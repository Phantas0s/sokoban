(ns sokoban.move
  (:require [sokoban.utils :as u]))

(defn direction [k]
  (k {:left [1 0]
      :right [-1 0]
      :up [1 0]
      :down [0 1]
      nil [0 0]}))

(defn calc
  [[x y] game]
  (let [[tile-width tile-height] (u/game-tile game)]
    ((+ x tile-width) (+ y tile-height))))

(defn move
  [game {:keys [:pressed-keys :player-x :player-y] :as state}]
  (println (direction (first pressed-keys)))
  (let [x (calc (direction (first pressed-keys)) game)]
    (println x)
    (assoc state
           {:player-x x}
           state)))
