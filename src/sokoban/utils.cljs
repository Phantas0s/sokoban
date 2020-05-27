(ns sokoban.utils)

(defn get-image [fname callback]
  (let [image (js/Image.)]
    (set! (.-src image) fname)
    (set! (.-onload image) #(callback {:data image
                                       :width image.width
                                       :height image.height}))))
; equivalent to
;     (doto image
;       (-> .-src (set! fname))
;       (-> .-onload (set! #(callback {:data image
;                                      :width image.width
;                                      :height image.height}))))))
