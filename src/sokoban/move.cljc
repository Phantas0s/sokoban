(ns sokoban.move
  (:require [sokoban.utils :as u]
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

(defn collision? [game tiled-map name new-pos]
  (let [obj (into [] (filter #(= (% :layer) name) (:tiles tiled-map)))]
    (filter #(= (:pos %) new-pos) obj)))

(defn remove-tile [game tiled-map name new-pos]
  (into [] (remove #(and (= (:layer name)) (= (:pos %) new-pos)) (:tiles tiled-map))))

(defn axis-add
  [[x1 y1] [x2 y2]]
  [(+ x1 x2) (+ y1 y2)])

(defn move-box [game tiled-map new-pos]
  (let [objs (into [] (filter #(= (% :layer) "boxes") (:tiles tiled-map)))
        box (into {} (collision? game tiled-map "boxes" new-pos))
        new-box (assoc box :pos (axis-add (:pos box) new-pos))]
    (assoc tiled-map :tiles (into (remove-tile game tiled-map "boxes" new-pos) [new-box]))))

(defn move-tile
  [{:keys [tile-width tile-height] :as game} [dir-x dir-y] [obj-x obj-y]]
  (let [new-x (+ obj-x dir-x)
        new-y (+ obj-y dir-y)]
    (if (in-context? game [new-x new-y])
      [new-x new-y]
      [obj-x obj-y])))

(defn move
  [game tiled-map {:keys [pressed-keys player-pos] :as state}]
  (if (empty? pressed-keys)
    state
    (let [k (first pressed-keys)
          next-direction (k direction)
          new-pos (move-tile game next-direction player-pos)
          new-states (assoc state :pressed-keys #{} :player-pos new-pos)]
      (cond (not-empty (collision? game tiled-map "walls" new-pos)) state
            (not-empty (collision? game tiled-map "boxes" new-pos)) (assoc new-states :tiled-map (move-box game tiled-map new-pos))
            :else new-states))))
