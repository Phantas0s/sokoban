(ns sokoban.move
  (:require
   [clojure.pprint :refer [pprint]]))

(def direction
  {:left [-1 0]
   :right [1 0]
   :up [0 -1]
   :down [0 1]})

(defn in-context?
  "Verify if the position given is still in the game"
  [{:keys [context tile-width tile-height]} [x y]]
  (let [width (/ (-> context .-canvas .-width) tile-width)
        height (/ (-> context .-canvas .-height) tile-height)]
    (and (>= x 0) (>= y 0) (< x width) (< y height))))

(defn move-object
  "Given a direction and a current position, return the new position"
  [game [dir-x dir-y] [obj-x obj-y]]
  (let [new-x (+ obj-x dir-x)
        new-y (+ obj-y dir-y)]
    (if (in-context? game [new-x new-y])
      [new-x new-y]
      [obj-x obj-y])))

(defn player-move
  "Moving the player is the base of all interactions in the game"
  [game {:keys [tiled-map pressed-keys player-pos] :as state}]
  (if (empty? pressed-keys)
    state
    (let [direction ((first pressed-keys) direction)
          new-pos (move-object game direction player-pos)]
      (assoc state :pressed-keys #{} :player-moves {:pos player-pos :new-pos new-pos :direction direction}))))
; )
