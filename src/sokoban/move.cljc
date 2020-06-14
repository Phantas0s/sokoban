(ns sokoban.move
  (:require
   [play-cljc.transforms :as t]
   [sokoban.tiles :as ti]
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

(defn collision? [tiled-map layer-name new-pos]
  (ti/tile-from-position tiled-map layer-name new-pos))

(defn collisions? [tiled-map layers new-pos]
  (some false? (map #(nil? (collision? tiled-map % new-pos)) layers)))

(defn move-object
  "Given a direction and a current position, return the new position"
  [game [dir-x dir-y] [obj-x obj-y]]
  (let [new-x (+ obj-x dir-x)
        new-y (+ obj-y dir-y)]
    (if (in-context? game [new-x new-y])
      [new-x new-y]
      [obj-x obj-y])))

(defn box-sprite [tiled-map layer [old-x old-y] [x y]]
  (if (seq (collision? tiled-map layer (vector x y)))
    (t/translate (nth (:tilesheet tiled-map) 14) old-x old-y)
    (t/translate (nth (:tilesheet tiled-map) 1) old-x old-y)))

(defn move-tile
  "Moving a tile given a layer, a direction and a predicate to prevent moving"
  [game tiled-map tile direction prevent-move? sprite]
  (let [tile-id (ti/tile-id tiled-map tile)
        new-pos (move-object game direction (:pos tile))]
    (if (prevent-move? new-pos)
      false
      (ti/move-tile tiled-map tile tile-id new-pos direction sprite))))

(defn player-interactions
  "Everything the player can interact with"
  [game tiled-map direction new-player-pos]
  (let [box-tile (ti/tile-from-position tiled-map "boxes" new-player-pos)]
    (if (seq (collision? tiled-map "boxes" new-player-pos))
      (move-tile game tiled-map box-tile direction
                 (fn [new-pos] (collisions? tiled-map ["walls" "boxes"] new-pos))
                 (fn [new-pos] (box-sprite tiled-map "goals" (:pos box-tile) new-pos)))
      tiled-map)))

(defn player-move
  "Moving the player is the base of all interactions in the game"
  [game {:keys [tiled-map pressed-keys player-pos] :as state}]
  (if (empty? pressed-keys)
    state
    (let [direction ((first pressed-keys) direction)
          new-state (assoc state :pressed-keys #{})
          new-pos (move-object game direction player-pos)
          new-tiled-map (player-interactions game tiled-map direction new-pos)]
      (if (or (collision? tiled-map "walls" new-pos) (false? new-tiled-map))
        new-state
        (assoc new-state :player-pos new-pos :tiled-map new-tiled-map)))))
