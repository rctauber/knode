(ns knode.front-end.query-editor.pagination
  (:require [cljs.reader :as reader]
            [knode.front-end.util :as util]))

(def page (atom 0))
(def query (atom nil))

(defn send!
  []
  (-> js/$
      (.get "/api/query" (clj->js {:sparql @query :output-format "json" :page @page}))
      (.done (fn [data] (js->clj (.parse js/JSON data))))))

(defn new-query! [sparql]
  (reset! page 0)
  (reset! query sparql))

(defn more! []
  (let [prom (send!)]
    (swap! page inc)
    prom))
