(def nrepl-port 4000)
(require
 '[nrepl.server :as nrepl-server]
 '[clojure.java.io :as io])

(defonce nrepl-server (atom nil))

(defn nrepl-handler []
  (require 'cider.nrepl)
  (ns-resolve 'cider.nrepl 'cider-nrepl-handler))

(defn start-nrepl-server! []
  (reset!
   nrepl-server
   (nrepl-server/start-server :port nrepl-port
                              :handler (nrepl-handler)))
  (println "Cider nREPL server started on port" nrepl-port)
  (spit ".nrepl-port" nrepl-port))

(defn stop-nrepl-server! []
  (when (not (nil? @nrepl-server))
    (nrepl-server/stop-server @nrepl-server)
    (println "Cider nREPL server on port" nrepl-port "stopped")
    (reset! nrepl-server nil)
    (io/delete-file ".nrepl-port" true)))

(defmulti task first)

(defmethod task :default
  [[task-name]]
  (println "Unknown task:" task-name)
  (System/exit 1))

(require
 '[figwheel.main :as figwheel])

(defn delete-children-recursively! [f]
  (when (.isDirectory f)
    (doseq [f2 (.listFiles f)]
      (delete-children-recursively! f2)))
  (when (.exists f) (io/delete-file f)))

(defmethod task nil
  [_]
  (delete-children-recursively! (io/file "resources/public/main.out"))
  (start-nrepl-server!)
  (figwheel/-main "--build" "dev" "--repl"))

(task *command-line-args*)
