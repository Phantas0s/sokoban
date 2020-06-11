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
  "Move a game object which is not part of the tiled-map"
  [game [dir-x dir-y] [obj-x obj-y]]
  (let [new-x (+ obj-x dir-x)
        new-y (+ obj-y dir-y)]
    (if (in-context? game [new-x new-y])
      [new-x new-y]
      [obj-x obj-y])))

(defn move-tile [tiled-map layer [dir-x dir-y] tile]
  (let [tile-id (ti/tile-id tiled-map tile)
        new-box {:layer layer :pos (vector (+ (first (:pos tile)) dir-x) (+ (second (:pos tile)) dir-y))}
        new-entities (ti/move-tile-entity (:entities tiled-map) tile-id [dir-x dir-y])
        tme (reduce-kv i/assoc (:tile-map-entity tiled-map) new-entities)]
    (if (collisions? tiled-map ["walls" "boxes"] (:pos new-box))
      false
      (assoc {} :tiled-map (-> tiled-map
                               (update :tiles (fn [tiles] (into (conj (subvec tiles 0 tile-id) new-box) (subvec tiles (inc tile-id)))))
                               (assoc-in [:layers layer (first (:pos tile)) (second (:pos tile))] nil)
                               (assoc-in [:layers layer (first (:pos new-box)) (second (:pos new-box))] new-box)
                               (assoc :entities new-entities
                                      :tile-map-entity tme))))))

(defn move
  [game {:keys [tiled-map pressed-keys player-pos] :as state}]
  (if (empty? pressed-keys)
    state
    (let [k (first pressed-keys)
          next-direction (k direction)
          new-pos (move-object game next-direction player-pos)
          new-states (assoc state :pressed-keys #{} :player-pos new-pos)
          box-tile (ti/tile-from-position tiled-map "boxes" new-pos)]
      (cond (not-empty (collision? tiled-map "walls" new-pos)) state
            (not-empty (collision? tiled-map "boxes" new-pos)) (let [updated-tiled-map (move-tile tiled-map "boxes" next-direction box-tile)]
                                                                 (if (false? updated-tiled-map)
                                                                   state
                                                                   (merge new-states updated-tiled-map)))
            :else new-states))))
