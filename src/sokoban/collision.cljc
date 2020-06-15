(ns sokoban.collision
  (:require
   [sokoban.move :as move]
   [sokoban.tiles :as ti]
   [play-cljc.transforms :as t]))

(def box-sprite-id 1)
(def box-goal-sprite-id 14)

(defn collision? [tiled-map layer-name new-pos]
  (ti/tile-from-position tiled-map layer-name new-pos))

(defn collisions? [tiled-map layers new-pos]
  (some false? (map #(nil? (collision? tiled-map % new-pos)) layers)))

(defn box-sprite [tiled-map layer [old-x old-y] [x y]]
  (if (seq (collision? tiled-map layer (vector x y)))
    (t/translate (nth (:tilesheet tiled-map) box-goal-sprite-id) old-x old-y)
    (t/translate (nth (:tilesheet tiled-map) box-sprite-id) old-x old-y)))

(defn move-tile
  "Moving a tile given a layer, a direction and a predicate to prevent moving"
  [game tile prevent-move? sprite {:keys [player-moves tiled-map] :as state}]
  (let [tile-id (ti/tile-id tiled-map tile)
        direction (:direction player-moves)
        new-pos (move/move-object game direction (:pos tile))]
    (if (prevent-move? new-pos)
      state
      (assoc state :tiled-map (ti/move-tile tiled-map tile tile-id new-pos direction sprite)))))

(defn player-interactions
  "Everything the player can interact with"
  ; [game tiled-map direction new-player-pos]
  [game {:keys [player-moves tiled-map] :as state}]
  (let [new-pos (:new-pos player-moves)
        box-tile (ti/tile-from-position tiled-map "boxes" new-pos)]
    (cond (seq (collision? tiled-map "walls" new-pos)) state
          (seq (collision? tiled-map "boxes" new-pos))
          (move-tile game
                     box-tile
                     (fn [new-pos] (collisions? tiled-map ["walls" "boxes"] new-pos))
                     (fn [new-pos] (box-sprite tiled-map "goals" (:pos box-tile) new-pos))
                     state)
          :else (assoc state :player-pos new-pos))))
