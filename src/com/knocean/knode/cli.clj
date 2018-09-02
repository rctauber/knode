(ns com.knocean.knode.cli
  (:require [clojure.java.io :as io]

            [com.knocean.knode.state :as st]
            [com.knocean.knode.server :as server]
            [com.knocean.knode.loader :as loader])
  (:gen-class))

(def usage "Usage: knode TASK

  knode serve    Serve resources
  knode config   Show configuration
  knode load [resource] [files]
  knode db (create|drop) (tables|indexes)
  knode help     Show this help message")

(defn -main [& args]
  (let [task (first args)]
    (case task
      "help" (println usage)
      "config" (do (st/init) (println (st/report @st/state)))
      "serve" (do (st/init) (server/start))

      "load"
      (do (st/init)
          (when (empty? (rest args))
            (let [dir (:absolute-dir @st/state)
                  load-file (io/as-file (str dir "/load.txt"))]
              (when-not (.exists load-file)
                (println "No load.txt exists in " dir " ...")
                (println "  you must specify a project and files to load")
                (System/exit 1))
              (println "Woo! doing the thing!")
              (let [lines (->> load-file
                               slurp
                               clojure.string/split-lines
                               (map #(str dir "/ontology/" %)))]
                (doseq [path lines]
                  (when-not (.exists (io/as-file path))
                    (println "Load file at " dir " refers to nonexistent file " path)
                    (System/exit 1)))
                (loader/load-resource (:project-name @st/state) lines)
                (System/exit 0))))
          (when (nil? (second args))
            (println "You must specify a project to load")
            (System/exit 1))
          (when (empty? (drop 2 args))
            (println "You must specify some files to load")
            (System/exit 1))
          (doseq [path (drop 2 args)]
            (when-not (.exists (io/as-file path))
              (println "File '" path "' does not exist")
              (System/exit 1)))
          (loader/load-resource (second args) (drop 2 args)))
      
      "db"
      (do (st/init)
          (case (->> args rest (take 2))
            ["create" "tables"] (loader/create-tables)
            ["drop" "tables"] (loader/drop-tables)
            ["create" "indexes"] (loader/create-indexes)
            ["drop" "indexes"] (loader/drop-indexes)
            (do (apply println "ERROR: Unknown db tasks:" (->> args rest (take 2)))
                (println usage)
                (System/exit 1))))

      (do (println (format "ERROR: Unknown task '%s'" task))
          (println usage)
          (System/exit 1)))))
