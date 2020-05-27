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

(def player-width 32)
(def player-height 32)

(def parsed-tiled-map (edn/read-string (read-tiled-map "character.tmx")))

(defn init [game]
  (gl game enable (gl game BLEND))
  (gl game blendFunc (gl game SRC_ALPHA) (gl game ONE_MINUS_SRC_ALPHA))
  (utils/get-image "character.png"
                   (fn [{:keys [data width height]}]
                     (let [entity (c/compile game (e/->image-entity game data width height))
                           images (vec (for [i (range 5)]
                                         (t/crop entity
                                                 (* i player-width)
                                                 0
                                                 player-width
                                                 player-height)))
                           [image1] images]
        ;; add it to the state
                       (swap! *state update :player-images assoc
                              :image1 image1)))))

(def screen-entity
  {:viewport {:x 0 :y 0 :width 0 :height 0}
   :clear {:color [(/ 173 255) (/ 216 255) (/ 230 255) 1] :depth 1}})

; need to export tileset with image (preference in tiled)

(defn tick [game] game
  (let [box (:image1 (:player-images @*state))
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
