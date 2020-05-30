(ns sokoban.move
  (:require [sokoban.utils :as u]))

(def direction
  {:left [-1 0]
   :right [1 0]
   :up [0 -1]
   :down [0 1]})

(defn in-context? [{:keys [context] :as game} [x y]]
  (let [width (-> context .-canvas .-width)
        height (-> context .-canvas .-height)]
    (println x y)
    (and (>= x 0) (>= y 0) (< x width) (< y height))))

(defn move-tile
  [{:keys [tile-width tile-height] :as game} [dir-x dir-y] [obj-x obj-y]]
  (let [new-x (+ obj-x (* dir-x tile-width))
        new-y (+ obj-y (* dir-y tile-height))]
    (if (in-context? game [new-x new-y])
      [new-x new-y]
      [obj-x obj-y])))

(defn move
  [game {:keys [pressed-keys player-x player-y] :as state}]
  (if (empty? pressed-keys)
    state
    (let [next-direction ((first pressed-keys) direction)
          [new-x new-y] (move-tile game next-direction [player-x player-y])]
      (assoc state
             :pressed-keys #{}
             :player-x new-x
             :player-y new-y))))
