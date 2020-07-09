(ns sokoban.start
  (:require [sokoban.core :as c]
            [play-cljc.gl.core :as pc]
            [goog.events :as events]
            [goog.events.KeyCodes]))

(def canvas-width 800)
(def canvas-height 800)
(def tile-width 32)
(def tile-height 32)

(defn resize [context]
  (set! context.canvas.width canvas-width)
  (set! context.canvas.height canvas-height))

(defn msec->sec [n]
  (* 0.001 n))

; practical but doesn't work with optimization advanced
; keys are named pretty randomly
(defn keycodes
  [k]
  (if-let [keycode (get (js->clj goog.events.KeyCodes) (str "$" k "$"))]
    keycode
    (get (js->clj goog.events.KeyCodes) k)))

; see https://keycode.info/
(defn keycode->keyword [keycode]
  (condp = keycode
    ; arrow keys
    37 :left
    39 :right
    38 :up
    40 :down

    ; hjkl
    72 :left
    76 :right
    75 :up
    74 :down

    8 :backspace
    82 :restart
    nil))

(defn game-loop [game]
  (let [game (c/tick game)]
    (js/requestAnimationFrame
     (fn [ts]
       (let [ts (msec->sec ts)]
         (game-loop (assoc game
                           :delta-time (- ts (:total-time game))
                           :total-time ts)))))))

(defn listen-for-keys []
  (events/listen js/window "keydown"
                 (fn [event]
                   (when-let [k (keycode->keyword (.-keyCode event))]
                     (swap! c/*state update :pressed-keys conj k))))
  (events/listen js/window "keyup"
                 (fn [event]
                   (when-let [k (keycode->keyword (.-keyCode event))]
                     (swap! c/*state update :pressed-keys disj k)))))

(defn start-game [context]
  (let [initial-game (assoc (pc/->game context)
                            :tile-width tile-width
                            :tile-height tile-height
                            :delta-time 0
                            :total-time (msec->sec (js/performance.now)))]
    (c/init initial-game)
    (game-loop initial-game)
    context))

(defonce context
  (let [canvas (js/document.querySelector "canvas")
        context (.getContext canvas "webgl2")]
    (listen-for-keys)
    (resize context)
    (start-game context)))
