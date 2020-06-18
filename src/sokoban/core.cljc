(ns sokoban.core
  (:require [sokoban.utils :as utils]
            [sokoban.move :as move]
            [sokoban.collision :as coll]
            [sokoban.tiles :as ti]
            #?(:cljs [cljs.reader :refer [read-string]]
               :clj [clojure.core :refer [read-string]])
            [play-cljc.gl.core :as c]
            [play-cljc.gl.entities-2d :as e]
            [play-cljc.transforms :as t]
            [clojure.pprint :refer [pprint]]
            [clojure.edn :as edn]
            #?(:clj  [play-cljc.macros-java :refer [gl math]]
               :cljs [play-cljc.macros-js :refer-macros [gl math]])
            #?(:clj  [sokoban.tiles :as tiles :refer [read-tiled-map]]
               :cljs [sokoban.tiles :as tiles :refer-macros [read-tiled-map]])))

(def level-1 (read-tiled-map "level1.tmx"))
(def level-2 (read-tiled-map "level2.tmx"))
; (def level-3 (read-tiled-map "level3.tmx"))

(defonce *state (atom {:pressed-keys #{}
                       :player-pos []
                       :level 0
                       :levels (vector level-1 level-2)
                       :player-moves {}
                       :tiled-map {}
                       :player-images {}}))

(defn read-level [level]
  (edn/read-string level))

(defn load-level [game {:keys [level levels] :as state}]
  (tiles/load-tiled-map game (read-level (nth levels level))
                        (fn [tiled-map]
                          (let [player-start (into {} (filter #(= (% :layer) "player-start") (-> tiled-map :tiles)))]
                            (swap! *state assoc :tiled-map tiled-map
                                   :player-pos (:pos player-start))))))

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
                           [image1] images]
                       (swap! *state assoc
                              :player-images {:image1 image1}))))
  (load-level game @*state))

(def screen-entity
  {:viewport {:x 0 :y 0 :width 0 :height 0}
   :clear {:color [(/ 255 255) (/ 255 255) (/ 255 255) 1] :depth 1}})

(defn win? [game {:keys [tiled-map level levels] :as state}]
  (if (ti/same-position? tiled-map ["boxes" "goals"])
    (load-level game state)
    state))

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
         (win? game))
    game))
