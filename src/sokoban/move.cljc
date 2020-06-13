(ns sokoban.move
  (:require
   [sokoban.tiles :as ti]
   [clojure.pprint :refer [pprint]]))

(def direction
  {:left [-1 0]
   :right [1 0]
   :up [0 -1]
   :down [0 1]})

(defn in-context? [{:keys [context tile-width tile-height] :as game} [x y]]
  "Verify if the position given is still in the game"
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

(defn move-tile [game tiled-map tile layer direction prevent-move?]
  "Moving a tile given a layer, a direction and a predicate to prevent moving"
  (let [tile-id (ti/tile-id tiled-map tile)
        tile-moved {:layer layer :pos (move-object game direction (:pos tile))}]
    (if (prevent-move? (:pos tile-moved))
      false
      (ti/move-tile tiled-map tile layer tile-id direction tile-moved))))

(defn player-interactions [game tiled-map direction new-player-pos]
  "Everything the player can interact with"
  (let [box-tile (ti/tile-from-position tiled-map "boxes" new-player-pos)]
    (if (not-empty (collision? tiled-map "boxes" new-player-pos))
      (move-tile game tiled-map box-tile "boxes" direction (fn [new-pos] (collisions? tiled-map ["walls" "boxes"] new-pos)))
      tiled-map)))

(defn player-move
  "Moving the player is the base of all interactions in the game"
  [game {:keys [tiled-map pressed-keys player-pos] :as state}]
  (if (empty? pressed-keys)
    state
    (let [k (first pressed-keys)
          direction (k direction)
          new-state (assoc state :pressed-keys #{})
          new-pos (move-object game direction player-pos)
          new-tiled-map (player-interactions game tiled-map direction new-pos)]
      (if (or (collision? tiled-map "walls" new-pos) (false? new-tiled-map))
        new-state
        (assoc new-state :player-pos new-pos :tiled-map new-tiled-map)))))
