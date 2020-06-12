(ns sokoban.move
  (:require
   [sokoban.tiles :as ti]
   [play-cljc.instances :as i]
   [clojure.pprint :refer [pprint]]))

(def direction
  {:left [-1 0]
   :right [1 0]
   :up [0 -1]
   :down [0 1]})

(defn in-context? [{:keys [context tile-width tile-height] :as game} [x y]]
  (let [width (/ (-> context .-canvas .-width) tile-width)
        height (/ (-> context .-canvas .-height) tile-height)]
    (and (>= x 0) (>= y 0) (< x width) (< y height))))

(defn collision? [tiled-map layer-name new-pos]
  (ti/tile-from-position tiled-map layer-name new-pos))

(defn collisions? [tiled-map layers new-pos]
  (some false? (map #(nil? (collision? tiled-map % new-pos)) layers)))

(defn move-object
  [game [dir-x dir-y] [obj-x obj-y]]
  (let [new-x (+ obj-x dir-x)
        new-y (+ obj-y dir-y)]
    (if (in-context? game [new-x new-y])
      [new-x new-y]
      [obj-x obj-y])))

(defn move-tile [game tiled-map layer direction tile prevent-move]
  (let [tile-id (ti/tile-id tiled-map tile)
        new-pos {:layer layer :pos (move-object game direction (:pos tile))}]
    (if (prevent-move (:pos new-pos))
      false
      (-> tiled-map
          (assoc-in [:layers layer (first (:pos tile)) (second (:pos tile))] nil)
          (assoc-in [:layers layer (first (:pos new-pos)) (second (:pos new-pos))] new-pos)
          (ti/move-tile-entity tile-id direction new-pos)))))

(defn player-interactions [game tiled-map direction new-player-pos]
  (let [box-tile (ti/tile-from-position tiled-map "boxes" new-player-pos)]
    (if (not-empty (collision? tiled-map "boxes" new-player-pos))
      (move-tile game tiled-map "boxes" direction box-tile (fn [new-pos] (collisions? tiled-map ["walls" "boxes"] new-pos)))
      tiled-map)))

(defn player-move
  "Moving the player is the base of all interactions in the game"
  [game {:keys [tiled-map pressed-keys player-pos] :as state}]
  (if (empty? pressed-keys)
    state
    (let [k (first pressed-keys)
          direction (k direction)
          new-state (assoc state :pressed-keys #{})
          new-pos (move-object game direction player-pos)]
      (if (collision? tiled-map "walls" new-pos)
        new-state
        (let [new-tiled-map (player-interactions game tiled-map direction new-pos)]
          (if (false? new-tiled-map)
            new-state
            (assoc new-state :player-pos new-pos :tiled-map new-tiled-map)))))))
