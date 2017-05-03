(ns leiningen.essthree
  (:require [lein-essthree.directory :as ed]
            [lein-essthree.library :as el]
            [lein-essthree.s3 :as s3]
            [lein-essthree.schemas :refer [EssthreeConfig]]
            [lein-essthree.uberjar :as eu]
            [leiningen.core.main :as main]
            [schema.core :as s]))


(s/defn ^:private get-config :- EssthreeConfig
  [project]
  (:essthree project))

(s/defn ^:private valid-bucket? :- s/Bool
  [config]
  (let [aws-creds (or (get-in config [:deploy :aws-creds])
                      (get-in config [:repository :aws-creds]))
        bucket    (or (get-in config [:deploy :bucket])
                      (get-in config [:repository :bucket]))]
    (s3/bucket-exists? aws-creds bucket)))

(defn essthree
  [project & args]
  (if-let [config (get-config project)]
    (if (valid-bucket? config)
      (case (get-in config [:deploy :type])
        :directory (ed/deploy-directory project)
        :library   (el/deploy-library project)
        :uberjar   (eu/deploy-uberjar project (get-in config [:deploy :uberjar-type]))
        (main/abort "Invalid deployment type specified:"
                    (get-in config [:deploy :type])))
      (main/abort "Invalid bucket specified:" (:bucket config)))
    (main/abort "No valid deployment configuration found.")))
