(ns sokoban.core
  (:require [sokoban.utils :as utils]
            [play-cljc.gl.core :as c]
            [play-cljc.gl.entities-2d :as e]
            [play-cljc.transforms :as t]
            [tile-soup.core :as ts]
            [clojure.edn :as edn]
            #?(:clj  [play-cljc.macros-java :refer [gl math]]
               :cljs [play-cljc.macros-js :refer-macros [gl math]])
            #?(:clj  [sokoban.tiles :as tiles :refer [read-tiled-map]]
               :cljs [sokoban.tiles :as tiles :refer-macros [read-tiled-map]])))

(defonce *state (atom {:pressed-keys #{}
                       :player-x 0
                       :player-y 0
                       :direction :right
                       :player-images {}}))

(defn load-tiled-map [game parsed]
  (let [map-width (-> parsed :attrs :width)
        map-height (-> parsed :attrs :height)
        tileset (first (filter #(= :tileset (:tag %)) (:content parsed)))
        image (first (filter #(= :image (:tag %)) (:content tileset)))
        {{:keys [tilewidth tileheight]} :attrs} tileset]
    ; crop tileset
    (utils/get-image (-> image :attrs :source)
                     (fn [{:keys [data width height]}]
                       (let [entity (e/->image-entity game data width height)
                             tiles-vert (/ height tileheight)
                             tiles-horiz (/ width tilewidth)
                             images (vec
                                     (for [y (range tiles-vert)
                                           x (range tiles-horiz)]
                                       (do (t/crop entity
                                                   (* x tilewidth)
                                                   (* y tileheight)
                                                   tilewidth
                                                   tileheight)
                                           (assoc entity :width tilewidth :height tileheight))))]
                         (run! (fn [i] (c/compile game i)) images)
                         (println (:x (nth images 2)))
                         (swap! *state update :player-images assoc images))))))

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
  (let [player (:player-images @*state)]
    (c/render game (update screen-entity :viewport
                           assoc :width 800 :height 800))
    (c/render game
              (-> player
                  (t/project 800 800)
                  (t/translate 0 0)
                  (t/scale 32 32)))
    game))


