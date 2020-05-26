(ns sokoban.core
  (:require [sokoban.utils :as utils]
            [play-cljc.gl.core :as c]
            [play-cljc.gl.entities-2d :as e]
            [play-cljc.transforms :as t]
            [clojure.pprint :refer [pprint]]
            [play-cljc.instances :as i]
            [clojure.edn :as edn]
            #?(:clj  [play-cljc.macros-java :refer [gl math]]
               :cljs [play-cljc.macros-js :refer-macros [gl math]])
            #?(:clj  [sokoban.tiles :as tiles :refer [read-tiled-map]]
               :cljs [sokoban.tiles :as tiles :refer-macros [read-tiled-map]])))

(defonce *state (atom {:pressed-keys #{}
                       :player-x 0
                       :player-y 0
                       :tileset
                       :player-images}))

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
                             [_ box] images]
                         (swap! *state update :player-images assoc :box box)
                         (swap! *state assoc :tileset entity))))))

(def parsed-tiled-map (edn/read-string (read-tiled-map "character.tmx")))

(defn init [game]
  (gl game enable (gl game BLEND))
  (gl game blendFunc (gl game SRC_ALPHA) (gl game ONE_MINUS_SRC_ALPHA))
  (load-tiled-map game parsed-tiled-map))

(def screen-entity
  {:viewport {:x 0 :y 0 :width 0 :height 0}
   :clear {:color [(/ 173 255) (/ 216 255) (/ 230 255) 1] :depth 1}})

; need to export tileset with image (preference in tiled)

(defn tick [game] game
  (let [box (:box (:player-images @*state))
        tileset (:tileset @*state)]
    (c/render game (update screen-entity :viewport
                           assoc
                           :width (-> game :context .-canvas .-clientWidth)
                           :height (-> game :context .-canvas .-clientHeight)))
    (c/render game
              (-> box
                  (t/project 800 800)
                  (t/scale 32 32)))
    game))
