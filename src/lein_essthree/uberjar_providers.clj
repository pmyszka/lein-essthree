(ns lein-essthree.uberjar-providers
  (:require [leiningen.uberjar :as uj]
            [leiningen.ring.uberjar :as rj]
            [schema.core :as s]))

(def ^:private uberjar-providers {:ring rj/uberjar
                                  :default uj/uberjar})

(defn find-provider [provider]
  (get uberjar-providers provider :default))

