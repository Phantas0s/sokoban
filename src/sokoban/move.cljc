(ns sokoban.move
  (:require [sokoban.utils :as u]))

(defn direction [k]
  (k {:left [-1 0]
      :right [1 0]
      :up [0 -1]
      :down [0 1]}))

(defn move-tile
  [[x y] game]
  [(* x 64) (* y 64)])

(defn move
  [game {:keys [pressed-keys player-x player-y] :as state}]
  (if (empty? pressed-keys)
    state
    (let [next-direction (direction (first pressed-keys))
          coord (move-tile next-direction game)]
      (assoc state :player-x (+ player-x (first coord)) :player-y (+ player-y (second coord))))))
