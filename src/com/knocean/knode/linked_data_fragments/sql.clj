(ns com.knocean.knode.linked-data-fragments.sql
  (:require [clojure.edn :as edn]
            [clojure.string :as string]
            [clojure.set :as set]
            [clojure.java.jdbc :as sql]

            [org.knotation.util :as util]
            [org.knotation.rdf :as rdf]
            [org.knotation.object :as ob]
            
            [com.knocean.knode.linked-data-fragments.base :as base :refer [query query-stream]]))

;;; DUMMY DATA
;; (def db
;;   {:classname "org.sqlite.JDBC"
;;    :subprotocol "sqlite"
;;    :subname "resources/obi_core.db"})
;; (defn dummy-db!
;;   [db]
;;   (sql/with-db-connection [handle db]
;;     (sql/execute! handle ["drop table if exists ontology"])
;;     (sql/execute! handle [(sql/create-table-ddl
;;                            :ontology
;;                            (map (fn [name] [name :string])
;;                                 [:gi :si :sb :pi :oi :ob :ol :di :ln]))])
;;     (doseq [d (:maps @st/state)] (sql/insert! handle :ontology d))))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn -query->sql-where-clause
  [query]
  (let [relevant (base/remove-falsies (select-keys query [:gi :si :pi :oi :ol :ln :di]))]
    (when (not (empty? relevant))
      (cons
       (str " WHERE " (string/join " AND " (remove nil? (map #(str (name %) "=?") (keys relevant)))))
       (vals relevant)))))

(defn -query->sql-pagination [{:keys [per-page page]}]
  (str " " (string/join
            " "
            (remove
             nil? [(str "LIMIT " per-page)
                   (when (> page 0) (str "OFFSET " (* page per-page)))]))))

(defn query->sql
  [query]
  (let [base "SELECT * FROM ontology"
        [where & params] (-query->sql-where-clause query)
        pagination (-query->sql-pagination query)]
    (vec
     (cons
      (string/join [base where pagination])
      params))))

(defmethod query-stream :database
  [query data]
  (let [base "SELECT * FROM ontology"
        [where & params] (-query->sql-where-clause query)]
    (sql/with-db-connection [db data]
      (map
       #(into {} (filter second %))
       (sql/query
        db (vec
            (cons
             (string/join [base where])
             params)))))))

(defn count!
  [query db]
  (let [base "SELECT COUNT(*) FROM ontology"
        [where & params] (-query->sql-where-clause query)
        pagination (-query->sql-pagination query)]
    (second
     (first
      (first
       (sql/query
        db (vec
            (cons
             (string/join [base where pagination])
             params))))))))

(defmethod query :database ;; if we've got a database, we need to query it for matching entries
  [query data]
  (sql/with-db-connection [db data]
    {:total (count! query db) :per-page (:per-page query) :page (:page query)
     :items (vec (map #(into {} (filter second %)) (sql/query db (query->sql query))))}))
