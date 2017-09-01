(ns knode.util)

(defn tap! [label value]
  (println label " - " value)
  value)

(defn all? [seq]
  (every? identity seq))

(defn starts-with? [target prefix]
  (and (>= (count target) (count prefix))
       (every? identity (map = target prefix))))

(defn ends-with? [target prefix]
  (and (>= (count target) (count prefix))
       (every? identity (map = (reverse target) (reverse prefix)))))

(defn absolute-uri-string? [s]
  "Cribbed from http://stackoverflow.com/a/19709846/190887. Takes a string
   and returns true if it represents an absolute uri (false otherwise)"
  (not (not (re-find #"(?i)^(?:[a-z]+:)?//" s))))

(defn expand-iri [base iri]
  (if (absolute-uri-string? iri)
    iri
    (str base iri)))

(defn throw-exception
  "Given a sequence of arguments,
   throw a cross-platform exception."
  [& messages]
  (throw
   (#?(:clj Exception. :cljs js/Error.)
    (->> messages (map str) (clojure.string/join " ")))))


(defn vector-compare [[value1 & rest1] [value2 & rest2]]
  (let [result (compare value1 value2)]
    (cond
      (not (zero? result)) result
      (nil? value1) 0
      :else (recur rest1 rest2))))

(defn -prepare-string [s]
  (let [s (clojure.string/lower-case (or s ""))
        parts (vec (clojure.string/split s #"\d+"))
        numbers (->> (re-seq #"\d+" s)
                     (map #(Long/parseLong %))
                     (vec))]
    (vec (interleave (conj parts "") (conj numbers "")))))

(defn natural-compare [a b]
  (vector-compare (-prepare-string a) (-prepare-string b)))
