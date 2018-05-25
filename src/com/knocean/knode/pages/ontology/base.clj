(ns com.knocean.knode.pages.ontology.base
  (:require [clojure.string :as string]

            [com.knocean.knode.pages.mimetypes :as mime]

            [com.knocean.knode.state :refer [state] :as st]
            [org.knotation.link :as ln]))

(defmulti ontology-result mime/req->output-format)

(defn all-subjects
  []
  (->> @state
       :project-name
       (format "SELECT DISTINCT si FROM states WHERE rt='%s' ORDER BY si")
       st/query
       (map :si)
       (filter #(.startsWith % (:base-iri @state)))))

(defn parse-body-terms
  [req]
  (when (and (= :post (:request-method req))
             (= "GET" (get-in req [:params "method"]))
             (:body-string req))
    (rest (string/split-lines (:body-string req)))))

(defn parse-request-terms
  [env {:keys [params uri] :as req}]
  (let [->terms (fn [s]
                  (when s
                    (if-let [_operator (second (re-find #"^(in|eq)\." s))]
                      (string/split (subs s 3) #" +"))))]
    (map
     #(ln/->iri env %)
     (concat
      (when (.startsWith uri (str "/ontology/" (:project-name @state) "_"))
        [(-> uri
             (string/replace #"^/ontology/" "")
             (string/replace #"\.ttl$" "")
             (string/replace #"\.json$" "")
             (string/replace #"\.tsv$" "")
             (string/replace "_" ":"))])
      (->terms (get params "CURIE"))
      (->terms (get params "IRI"))
      (parse-body-terms req)))))

(defn with-requested-terms
  [f]
  (mime/with-content-header
    (fn [{:keys [params uri requested-iris] :as req}]
      (let [env (st/latest-env)
            remaining-path (vec (drop 2 (string/split (:uri req) #"/")))
            req (assoc req :body-string (when (:body req) (-> req :body slurp)))]
        (f (assoc
            req
            :env env
            :remaining-path remaining-path
            :requested-iris (or requested-iris (vec (parse-request-terms env req)))))))))
