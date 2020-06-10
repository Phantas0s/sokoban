(ns sokoban.tiles
  (:require
   [sokoban.utils :as utils]
   [play-cljc.transforms :as t]
   [play-cljc.math :as m]
   [play-cljc.instances :as i]
   [clojure.pprint :refer [pprint]]
   [play-cljc.gl.core :as c]
   [play-cljc.gl.entities-2d :as e]
   #?@(:clj [[clojure.java.io :as io]
             [tile-soup.core :as ts]])
   #?(:clj  [play-cljc.macros-java :refer [math]]
      :cljs [play-cljc.macros-js :refer-macros [math]])))

#?(:clj (defmacro read-tiled-map [fname]
          (-> (str "public/" fname)
              io/resource
              slurp
              ts/parse
              pr-str)))

(defn crop-tileset [tileset width height tilewidth tileheight]
  "Crop the tileset (must be included in the map) and return each tile as image"
  (let [tiles-vert (/ height tileheight)
        tiles-horiz (/ width tilewidth)]
    (vec
     (for [y (range tiles-vert)
           x (range tiles-horiz)]
       (t/crop tileset
               (* x tilewidth)
               (* y tileheight)
               tilewidth
               tileheight)))))

(defn load-tiled-map [game parsed callback]
  "Parse a map (tmx) from the tiled software"
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
                       (println layers)
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
                                                      {:layer layer-name :tile-x x :tile-y y})]
                                       (cond-> m
                                         true (assoc-in [:layers layer-name x y] tile-map)
                                         tile-map (update :tiles conj tile-map)
                                         tile-map (update :entities conj (t/translate (nth tiles-img image-id) x y)))))
                                   m
                                   (range (count layer)))))
                              {:layers {}
                               :tiles []
                               :entities []}
                              ["background" "walls" "boxes" "goals" "player-start"])
                             entity (i/->instanced-entity entity)
                             entity (c/compile game entity)
                             entity (reduce-kv i/assoc entity entities)]
                         (callback
                          {:layers layers
                           :tiles tiles
                           :entities entities
                           :map-width map-width
                           :map-height map-height}
                          entity))))))

(defn tile-from-name [tiled-map tile-name]
  (into {} (filter #(= (% :layer) tile-name) (:tiles tiled-map))))

(defn tile-from-position [tiled-map layer-name pos]
  (get-in (get (:layers tiled-map) layer-name) pos))

(defn tile-id [tile-map tile]
  (.indexOf (:tiles tile-map) tile))
