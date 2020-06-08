(ns sokoban.move
  (:require [sokoban.utils :as u]
            [play-cljc.gl.core :as c]
            [play-cljc.instances :as i]
            [clojure.pprint :refer [pprint]]))

(defn add-pos
  [[x1 y1] [x2 y2]]
  [(+ x1 x2) (+ y1 y2)])

(defn sub-pos
  [[x1 y1] [x2 y2]]
  [(- x2 x1) (+ y2 y1)])

(def direction
  {:left [-1 0]
   :right [1 0]
   :up [0 -1]
   :down [0 1]})

(defn in-context? [{:keys [context tile-width tile-height] :as game} [x y]]
  (let [width (/ (-> context .-canvas .-width) tile-width)
        height (/ (-> context .-canvas .-height) tile-height)]
    (and (>= x 0) (>= y 0) (< x width) (< y height))))

(defn get-tile [game tiled-map layer-name pos]
  (get-in (get (:layers tiled-map) layer-name) pos))

(defn collision? [game tiled-map layer-name new-pos]
  (get-tile game tiled-map layer-name new-pos))

(defn tile-id [game tile-map tile]
  (.indexOf (:tiles tile-map) tile))

(defn get-tile-entity [game tile-map tile]
  (nth (:tiles tile-map) (.indexOf (:tiles tile-map) tile)))

(defn swap [v i1 i2]
  (assoc v i2 (v i1) i1 (v i2)))

(defn move-tile
  [{:keys [tile-width tile-height] :as game} [dir-x dir-y] [obj-x obj-y]]
  (let [new-x (+ obj-x dir-x)
        new-y (+ obj-y dir-y)]
    (if (in-context? game [new-x new-y])
      [new-x new-y]
      [obj-x obj-y])))

(defn move-box [game tiled-map tiled-map-entity dir tile]
  (let [tile-id (tile-id game tiled-map tile)
        old-pos [(:tile-x tile) (:tile-y tile)]
        tme (i/dissoc tiled-map-entity tile-id)
        tme (reduce-kv i/assoc tme (:entities tiled-map))]
    (assoc {} :tiled-map-entity tme
           :tiled-map (-> tiled-map
                          (update :tiles (fn [tiles]
                                           (into (subvec tiles 0 tile-id) (subvec tiles (inc tile-id)))))
                          (assoc-in [:layers "boxes" 1 3] nil)))))

(defn move
  [game {:keys [tiled-map tiled-map-entity pressed-keys player-pos] :as state}]
  (if (empty? pressed-keys)
    state
    (let [k (first pressed-keys)
          next-direction (k direction)
          new-pos (move-tile game next-direction player-pos)
          new-states (assoc state :pressed-keys #{} :player-pos new-pos)
          box-tile (get-tile game tiled-map "boxes" new-pos)]

      (pprint (:tiles tiled-map))
      (cond (not-empty (collision? game tiled-map "walls" new-pos)) state
            (not-empty (collision? game tiled-map "boxes" new-pos)) (merge new-states (move-box game tiled-map tiled-map-entity next-direction box-tile))
            :else new-states))))
