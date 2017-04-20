(ns knode.emit
  (:require [clojure.string :as string]
            [knode.core :as core]))

(defn emit-ttl-statement
  "Given a block-map with :predicate and :object,
   return a Turtle statement string."
  [env {:keys [predicate object] :as block}]
  (str
   (core/get-curie env (:iri predicate))
   " "
   (cond
     (:language object)
     (format "\"%s\"%s" (:lexical object) (:language object))
     (:datatype object)
     (format "\"%s\"^^%s"
             (:lexical object)
             (core/get-curie env (get-in object [:datatype :iri])))
     (:lexical object)
     (format "\"%s\"" (:lexical object))
     (core/get-curie env (:iri object))
     (core/get-curie env (:iri object))
     :else
     (format "<%s>" (:iri object)))))

(defn emit-ttl
  "Given a sequence of context block-maps (prefixes),
   a subject block-map, and a sequence of block-maps (statements),
   return a stand-alone Turtle string
   with prefixes and a stanza for that subject with those statements."
  [env context-blocks subject blocks]
  (str
   (->> context-blocks
        (filter :prefix)
        (map #(str "@prefix " (:prefix %) ": <" (:iri %) "> ."))
        (string/join "\n"))
   "\n\n"
   (core/get-curie env (:iri subject))
   "\n  "
   (->> blocks
        (filter :predicate)
        (map (partial emit-ttl-statement env))
        (string/join "\n; "))
   "\n."))

(defn emit-rdfa-statement
  "Given a block-map with :predicate and :object,
   return a list item using Hiccup for HTML+RDFa."
  [env {:keys [predicate object] :as block}]
  (let [property (core/get-curie env (:iri predicate))]
    [:li
     [:a {:href (:iri predicate)} (core/get-name env (:iri predicate))]
     ": "
     (cond
       (:language object)
       [:span
        {:property property
         :xml:lang (string/replace (:language object) #"^@" "")}
        (:lexical object)]

       (:datatype object)
       [:span
        {:property property
         :datatype (core/get-curie env (get-in object [:datatype :iri]))}
        (:lexical object)]

       (:lexical object)
       [:span {:property property} (:lexical object)]

       :else
       [:a
        {:href (:iri object) :property property}
        (core/get-name env (:iri object))])]))

(defn emit-rdfa
  "Given a sequence of context block-maps (prefixes),
   a subject block-map, and a sequence of block-maps (statements),
   return an HTML+RDFa <ul> element in Hiccup format."
  [env context-blocks subject blocks]
  (apply
   conj
   [:ul
    {:prefix
     (->> context-blocks
          (filter :prefix)
          (map #(str (:prefix %) ": " (:iri %)))
          (string/join "\n"))
     :resource (core/get-curie env (:iri subject))}]
   (->> blocks
        (filter :predicate)
        (map (partial emit-rdfa-statement env))
        vec)))
