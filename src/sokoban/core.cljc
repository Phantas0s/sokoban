(ns sokoban.core
  (:require [play-cljc.gl.core :as c]
            [play-cljc.gl.entities-2d :as e]
            [play-cljc.transforms :as t]
            [play-cljc.macros-js :refer-macros [gl math]]))

(defonce *state (atom {:pressed-keys #{}
                       :player-x 0
                       :player-y 0
                       :direction :right
                       :player-images {}
                       :player-image-key :walk1}))

(defn init [game]
  (gl game enable (gl game BLEND))
  (gl game blendFunc (gl game SRC_ALPHA) (gl game ONE_MINUS_SRC_ALPHA)))

(def screen-entity
  {:viewport {:x 0 :y 0 :width 0 :height 0}
   :clear {:color [(/ 173 255) (/ 216 255) (/ 230 255) 1] :depth 1}})

(defn tick [game] game)
