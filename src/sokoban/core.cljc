(ns sokoban.core
  (:require [sokoban.utils :as utils]
            [sokoban.move :as move]
            [sokoban.collision :as coll]
            [sokoban.tiles :as ti]
            [play-cljc.gl.core :as c]
            [play-cljc.gl.entities-2d :as e]
            [play-cljc.transforms :as t]
            [clojure.pprint :refer [pprint]]
            [clojure.edn :as edn]
            #?(:clj  [play-cljc.macros-java :refer [gl math]]
               :cljs [play-cljc.macros-js :refer-macros [gl math]])
            #?(:clj  [sokoban.tiles :as tiles :refer [read-tiled-map]]
               :cljs [sokoban.tiles :as tiles :refer-macros [read-tiled-map]])))

(defonce *state (atom {:pressed-keys #{}
                       :player-pos []
                       :player-moves {}
                       :tiled-map {}
                       :player-images {}}))

(def tiled-map (edn/read-string (read-tiled-map "level1.tmx")))

(defn init [game]
  (gl game enable (gl game BLEND))
  (gl game blendFunc (gl game SRC_ALPHA) (gl game ONE_MINUS_SRC_ALPHA))
  (utils/get-image "character.png"
                   (fn [{:keys [data width height]}]
                     (let [entity (c/compile game (e/->image-entity game data width height))
                           player-width (-> game :tile-width)
                           player-height (-> game :tile-height)
                           images (vec (for [i (range 5)]
                                         (assoc (t/crop entity
                                                        (* i player-width)
                                                        0
                                                        player-width
                                                        player-height) :width player-width :height player-height)))
                           [image1] images
                           player-start (into {} (filter #(= (% :layer) "player-start") (-> (:tiled-map @*state) :tiles)))]
                       (swap! *state assoc
                              :player-pos (:pos player-start)
                              :player-images {:image1 image1}))))
  (tiles/load-tiled-map game tiled-map
                        (fn [tiled-map]

                          (swap! *state assoc :tiled-map tiled-map))))

(def screen-entity
  {:viewport {:x 0 :y 0 :width 0 :height 0}
   :clear {:color [(/ 255 255) (/ 255 255) (/ 255 255) 1] :depth 1}})

(defn win? [layers {:keys [tiled-map]}]
  (pprint (ti/tile-from-layer tiled-map "boxes")))

(defn tick [game] game
  (let [{:keys [player-pos
                player-images
                tiled-map] :as state} @*state
        player (:image1 player-images)
        width (-> game :context .-canvas .-width)
        height (-> game :context .-canvas .-height)
        tile-width (-> game :tile-width)
        tile-height (-> game :tile-height)
        tile-map-entity (:tile-map-entity tiled-map)
        [player-x-pix player-y-pix] (utils/pos->pixel game player-pos)]
    (c/render game (update screen-entity :viewport
                           assoc
                           :width width
                           :height height))
    (c/render game (-> tile-map-entity
                       (t/project width height)
                       (t/scale tile-width tile-height)))
    (c/render game (-> player
                       (t/project width height)
                       (t/translate player-x-pix player-y-pix)
                       (t/scale tile-width tile-height)))
    (->> state
         (move/player-move game)
         (coll/player-interactions game)
         (reset! *state)
         (win? ["boxes"]))
    game))
