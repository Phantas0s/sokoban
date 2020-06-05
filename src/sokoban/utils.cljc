(ns sokoban.utils)

#?(:clj (defn get-image [fname callback]))
#?(:cljs (defn get-image [fname callback]
           (let [image (js/Image.)]
             (set! (.-src image) fname)
             (set! (.-onload image) #(callback {:data image
                                                :width image.width
                                                :height image.height})))))
; equivalent to
;     (doto image
;       (-> .-src (set! fname))
;       (-> .-onload (set! #(callback {:data image
;                                      :width image.width
;                                      :height image.height}))))))

(defn game-width [game]
  (-> game :context .-canvas .-clientWidth))

(defn game-height [game]
  (-> game :context .-canvas .-clientHeight))

(defn game-tile [game]
  (vec (/ (game-width game) (game-height game))))

(defn pos->pixel [game [x y]]
  [(* (:tile-width game) x) (* (:tile-height game) y)])
