(ns sokoban.tiles
  (:require
   [sokoban.utils :as utils]
   [play-cljc.transforms :as t]
   [play-cljc.math :as m]
   [play-cljc.instances :as i]
   [play-cljc.gl.core :as c]
   [play-cljc.gl.entities-2d :as e]
   #?@(:clj [[clojure.java.io :as io]
             [tile-soup.core :as ts]])
   #?(:clj  [play-cljc.macros-java :refer [math]]
      :cljs [play-cljc.macros-js :refer-macros [math]])))

(defn load-tiled-map [game parsed]
  (let [map-width (-> parsed :attrs :width)
        map-height (-> parsed :attrs :height)
        ; tileset image and properties
        tileset (first (filter #(= :tileset (:tag %)) (:content parsed)))
        ; tileset image
        image (first (filter #(= :image (:tag %)) (:content tileset)))
        ; layers of the map
        layers (->> parsed :content
                    (filter #(= :layer (:tag %)))
                    (map #(vector
                           (-> % :attrs :name)
                           (-> % :content first :content first)))
                    (into {}))
        {{:keys [tilewidth tileheight]} :attrs} tileset]
    ; crop tileset
    (utils/get-image (-> image :attrs :source)
                     (fn [{:keys [data width height]}]
                       (let [entity (c/compile game (e/->image-entity game data width height))
                             tiles-vert (/ height tileheight)
                             tiles-horiz (/ width tilewidth)
                             images (vec
                                     (for [y (range tiles-vert)
                                           x (range tiles-horiz)]
                                       (assoc (t/crop entity
                                                      (* x tilewidth)
                                                      (* y tileheight)
                                                      tilewidth
                                                      tileheight) :width tilewidth :height tileheight)))
                             [_ box] images
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
                                         tile-map (update :entities conj (t/translate (nth images image-id) x y)))))
                                   m
                                   (range (count layer)))))
                              {:layers {}
                               :tiles []
                               :entities []}
                              ["player"])]
                         ; (swap! *state update :player-images assoc :box box)(swap! *state assoc :tileset entity
                         )))))
