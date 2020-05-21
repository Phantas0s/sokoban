(ns sokoban.start
  (:require [sokoban.core :as c]
            [play-cljc.gl.core :as pc]
            [goog.events.KeyCodes]
            [goog.events :as events]))

(def canvas-width 800)
(def canvas-height 800)

(defn resize [context]
  (set! context.canvas.width canvas-width)
  (set! context.canvas.height canvas-height))

(defn msec->sec [n]
  (* 0.001 n))

(defn keycodes
  [k]
  (get (js->clj goog.events.KeyCodes :keywordize-keys true) k))

(defn game-loop [game]
  (let [game (c/tick game)]
    (js/requestAnimationFrame
     (fn [ts]
       (let [ts (msec->sec ts)]
         (game-loop (assoc game
                           :delta-time (- ts (:total-time game))
                           :total-time ts)))))))

(defn keycode->keyword [keycode]
  (condp = keycode
    (keycodes :LEFT) :left
    (keycodes :RIGHT) :right
    (keycodes :UP) :up
    (keycodes :DOWN) :down
    nil))

(defn listen-for-keys []
  (events/listen js/window "keydown"
                 (fn [event]
                   (when-let [k (keycode->keyword (.-keyCode event))]
                     (swap! c/*state update :pressed-keys conj k))))
  (events/listen js/window "keyup"
                 (fn [event]
                   (when-let [k (keycode->keyword (.-keyCode event))]
                     (swap! c/*state update :pressed-keys disj k)))))

(defonce context
  (let [canvas (js/document.querySelector "canvas")
        context (.getContext canvas "webgl2")
        initial-game (assoc (pc/->game context)
                            :delta-time 0
                            :total-time (msec->sec (js/performance.now)))]
    (listen-for-keys)
    (resize context)
    (c/init initial-game)
    (game-loop initial-game)
    context))
