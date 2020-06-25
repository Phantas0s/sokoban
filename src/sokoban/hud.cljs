(ns sokoban.hud)

(defn update-hud! [{:keys [level] :as state}]
  (let [el-level (.getElementById js/document "hud-level")]
    (set! (.-innerHTML el-level) level)))
