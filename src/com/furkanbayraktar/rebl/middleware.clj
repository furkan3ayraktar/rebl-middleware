(ns com.furkanbayraktar.rebl.middleware
  (:require [clojure.string :as str]
            [cognitect.rebl :as rebl]
            [nrepl.middleware.print :refer [wrap-print]]
            [nrepl.middleware :refer [set-descriptor!]]
            [nrepl.transport :as transport])
  (:import [nrepl.transport Transport]))

(defn- cursive?
  "Takes an nREPL request and returns true if the request is an eval request
   and the code passed starts with '(cursive.repl'."
  [request]
  (and (= (get request :op) "eval")
       (str/starts-with? (get request :code) "(cursive.repl")))

(defn send-to-rebl! [{:keys [code] :as req} {:keys [value] :as resp}]
  (when (and code value)
    (rebl/submit (read-string code) value))
  resp)

(defn- wrap-rebl-sender
  "Wraps a `Transport` where if the send function is invoked, it checks if it is
  a Cursive related code and skips sending that to REBL. Otherwise, it forwards the
  request to the REBL."
  [{:keys [id op ^Transport transport] :as request}]
  (reify transport/Transport
    (recv [this]
      (.recv transport))
    (recv [this timeout]
      (.recv transport timeout))
    (send [this resp]
      (.send transport
             (cond
              (cursive? request) resp
              :else (send-to-rebl! request resp)))
      this)))

(defn wrap-rebl [handler]
  (fn [request]
    (handler (assoc request :transport (wrap-rebl-sender request)))))

(set-descriptor! #'wrap-rebl
                 {:requires #{#'wrap-print}
                  :expects #{"eval"}
                  :handles {}})
