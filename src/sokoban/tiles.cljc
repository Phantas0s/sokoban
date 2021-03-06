(ns sokoban.tiles
  (:require
   [sokoban.utils :as utils]
   [play-cljc.transforms :as t]
   [play-cljc.instances :as i]
   [clojure.pprint :refer [pprint]]
   [play-cljc.gl.core :as c]
   [play-cljc.gl.entities-2d :as e]
   #?@(:clj [[clojure.java.io :as io]
             [tile-soup.core :as ts]])))

#?(:clj (defmacro read-tiled-map [level]
          (-> (str "public/levels/level" level ".tmx")
              io/resource
              slurp
              ts/parse
              pr-str)))

(defn crop-tileset [tileset width height tilewidth tileheight]
  "Crop the tileset (must be included in the parsed map) and return each tile as image"
  (let [tiles-vert (/ height tileheight)
        tiles-horiz (/ width tilewidth)]
    (vec
     ; (map #(t/crop tileset (* %1 tilewidth) (* %2 tileheight) tilewidth tileheight) (vec (range tiles-horiz)) (vec (range tiles-vert)))
     (for [y (range tiles-vert)
           x (range tiles-horiz)]
       (t/crop tileset
               (* x tilewidth)
               (* y tileheight)
               tilewidth
               tileheight)))))

(defn load-tiled-map
  "Parse a map (tmx) from the tiled software"
  [game parsed callback]
  (let [map-width (-> parsed :attrs :width) ; number of tiles
        map-height (-> parsed :attrs :height) ; number of tiles
        tileset (first (filter #(= :tileset (:tag %)) (:content parsed)))
        image (first (filter #(= :image (:tag %)) (:content tileset)))
        {{:keys [tilewidth tileheight]} :attrs} tileset
        layers (->> parsed :content
                    (filter #(= :layer (:tag %)))
                    (map #(vector
                           (-> % :attrs :name)
                           (-> % :content first :content first)))
                    (into {}))]
    (utils/get-image (-> image :attrs :source)
                     (fn [{:keys [data width height]}]
                       (let [entity (e/->image-entity game data width height)
                             tiles-img (crop-tileset entity width height tilewidth tileheight)
                             {:keys [layers tiles entities]}
                             (reduce
                              (fn [m layer-name]
                                (let [layer (get layers layer-name)]
                                  (reduce
                                   (fn [m i]
                                     (let [x (mod i map-width)
                                           y (int (/ i map-width))
                                           image-id (dec (nth layer i))
                                           tile-map (when (>= image-id 0)
                                                      {:layer layer-name :pos (vector x y)})]
                                       (cond-> m
                                         true (assoc-in [:layers layer-name x y] tile-map)
                                         tile-map (update :tiles conj tile-map)
                                         tile-map (update :entities conj (t/translate (nth tiles-img image-id) x y)))))
                                   m
                                   (range (count layer)))))
                              {:layers {}
                               :tiles []
                               :entities []}
                              ["background" "player-start" "goals" "walls"  "boxes"])
                             entity (i/->instanced-entity entity)
                             entity (c/compile game entity)
                             entity (reduce-kv i/assoc entity entities)]
                         (callback
                          {:layers layers
                           :tiles tiles
                           :tile-map-entity entity
                           :entities entities
                           :tilesheet tiles-img
                           :map-width map-width
                           :map-height map-height}))))))

(defn tile-from-layer
  [tiled-map layer]
  (filter #(= (:layer %) layer) (:tiles tiled-map)))

; TODO this SHOULD be decomposed but I'm kind of proud of it :D
(defn same-position?
  [tiled-map layers]
  (let [tiles (flatten (map (fn [layer] (filter (fn [tile] (= (:layer tile) layer)) (:tiles tiled-map))) layers))]
    ; (println (count (reduce (fn [v m] (conj v (:pos m))) #{} tiles)))
    ; (println (/ (count tiles) (count layers)))
    (= (/ (count tiles) (count layers)) (count (reduce (fn [v m] (conj v (:pos m))) #{} tiles)))))

(defn all-placed?
  [tiled-map box-layer goal-layer]
  (let [boxes (filter (fn [tile] (= (:layer tile) box-layer)) (:tiles tiled-map))
        goals (filter (fn [tile] (= (:layer tile) goal-layer)) (:tiles tiled-map))
        pos-goals (reduce (fn [v m] (conj v (:pos m))) #{} goals)
        pos-boxes (reduce (fn [v m] (conj v (:pos m))) #{} boxes)
        goal-boxes-not-places (reduce (fn [v m] (conj v (:pos m))) pos-goals boxes)]
    (= (count pos-boxes) (count goal-boxes-not-places))))

(defn tile-from-position
  "Get a tile from a layer and a position"
  [tiled-map layer pos]
  (get-in (get (:layers tiled-map) layer) pos))

(defn tile-id [tile-map tile]
  (.indexOf (:tiles tile-map) tile))

(defn move-tile [tiled-map tile tile-id new-pos [dir-x dir-y] sprite]
  (let [layer (:layer tile)
        [old-x old-y] (:pos tile)
        [new-x new-y] new-pos
        new-entity (sprite new-pos)
        tile-moved (assoc tile :pos new-pos)
        new-entities (into (conj (subvec (:entities tiled-map) 0 tile-id) (t/translate new-entity dir-x dir-y)) (subvec (:entities tiled-map) (inc tile-id)))]
    (-> tiled-map
        (assoc-in [:layers layer old-x old-y] nil)
        (assoc-in [:layers layer new-x new-y] tile-moved)
        (assoc
         :tiles (into (conj (subvec (:tiles tiled-map) 0 tile-id) tile-moved) (subvec (:tiles tiled-map) (inc tile-id)))
         :entities new-entities
         :tile-map-entity (reduce-kv i/assoc (:tile-map-entity tiled-map) new-entities)))))
