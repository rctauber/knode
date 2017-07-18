(ns knode.server.query
  (:require
   [clojure.java.io :as io]
   [clojure.string :as string]
   [clojure.edn :as edn]

   [knode.state :refer [state]]
   [knode.sparql :as sparql]
   [knode.server.template :as pg]
   [knode.server.util :as util]))

(def +default-limit+ 200)

(defn -ensure-limit [query]
  (if-let [res (re-find #"(?i)LIMIT (\d+)" query)]
    (let [given-limit (read-string (second res))]
      (if (> given-limit +default-limit+)
        [(string/replace query (re-pattern (first res)) (str "LIMIT " +default-limit+)) +default-limit+]
        [query given-limit]))
    [(str query "\nLIMIT " +default-limit+) +default-limit+]))

(defn -ensure-offset [query limit page]
  (if (or (= 0 page) (re-find #"(?i)OFFSET (\d+)" query))
    query
    (str query "\nOFFSET " (* limit page))))

(defn sanitized-sparql
  [query & {:keys [page]}]
  (let [[query limit] (-ensure-limit query)]
    (-ensure-offset query limit (or page 0))))

(defn render-default-queries
  [req]
  {:status 200
   :headers {"Content-Type" "application/edn"}
   :body (try
           (str (edn/read (java.io.PushbackReader. (io/reader (str (:ontology-dir @state))))))
           (catch Exception e
             (str ["SELECT * WHERE { ?s ?p ?o }"])))})

(defn render-query-interface
  [req]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (pg/base-template
          req
          {:title "Query Interface"
           :content [:div
                     [:div {:id "editor"}]
                     [:button {:class "send-query"} "Query"]
                     [:div {:id "result"}]
                     [:button {:class "more-results"} "More"]
                     [:script {:src "/assets/ace/ace.js" :type "text/javascript" :charset "utf-8"}]]})})
