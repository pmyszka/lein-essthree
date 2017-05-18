(ns lein-essthree.uberjar
  "Deploy project as an application uberjar to S3."
  (:require [cuerdas.core :as c]
            [leiningen.core.main :as main]
            [lein-essthree.s3 :as s3]
            [lein-essthree.schemas :refer [UberjarDeployConfig]]
            [schema.core :as s]
            [pandect.algo.md5 :as md5]
            [lein-essthree.uberjar-providers :as ujp])
  (:import [com.amazonaws AmazonServiceException]))

(s/defn ^:private get-config :- UberjarDeployConfig
  [project]
  (get-in project [:essthree :deploy]))

(s/defn ^:private compile-uberjar! :- s/Str
  [project uberjar-provider]
  (let [uj-method (ujp/find-provider uberjar-provider)]
    (uj-method project)))

(s/defn ^:private put-uberjar-s3! :- (s/maybe s/Str)
  [config  :- UberjarDeployConfig
   uj-path :- s/Str]
  (let [aws-creds    (:aws-creds config)
        bucket       (:bucket config)
        path         (c/trim (:path config) "/")
        uj-artifact  (or (:artifact-name config)
                         (last (c/split uj-path "/")))
        obj-key      (->> [path uj-artifact]
                          (filter identity)
                          (c/join "/"))
        md5file       (str uj-path ".md5")]
    (try
      (s3/put-file! aws-creds bucket obj-key uj-path)
      (spit md5file (md5/md5-file uj-path))
      (s3/put-file! aws-creds bucket (str obj-key ".md5") md5file)
      (str bucket "/" obj-key)
      (catch AmazonServiceException e
        (main/abort "Uberjar upload to S3 failed with:"
                    (:message (amazonica.core/ex->map e)))))))

(defn deploy-uberjar
  [project uberjar-type]
  "Deploy the current project as an application uberjar to S3."
  (let [config  (get-config project)
        uj-path (compile-uberjar! project uberjar-type)
        uj-obj  (put-uberjar-s3! config uj-path)]
    (when uj-obj
      (main/info "Uploaded uberjar to" uj-obj))))
