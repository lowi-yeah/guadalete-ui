(ns guadalete-ui.helpers.session
    (:require [clojure.core.cache :as cache]
      [clj-uuid :as uuid]
      [expiring-map.core :as em]
      [taoensso.timbre :refer [tracef debugf infof warnf errorf]])
    (:use ring.middleware.session.store))

;; session cache to maintain authentication
;; 5 minutes of inactive will log you out
;(def session-map (atom (cache/ttl-cache-factory {} :ttl (minutes 5))))
(def session-map (em/expiring-map
                   60 {:expiration-policy :access :time-unit :minutes}))

(deftype ExpiringMapStore [session-map]
         SessionStore

         (read-session [_ key]
                       "Read a session map from the store. If the key is not found, nil is returned."
                       (get session-map key nil))

         (write-session [_ key data]
                        "Write a session map to the store. Returns the (possibly changed) key under
                        which the data was stored. If the key is nil, the session is considered
                        to be new, and a fresh key should be generated."
                        (let [key (or key (str (uuid/v4)))]
                             (em/assoc! session-map key data)
                             key))

         (delete-session [_ key]
                         "Write a session map to the store. Returns the (possibly changed) key under
                         which the data was stored. If the key is nil, the session is considered
                         to be new, and a fresh key should be generated."
                         (em/dissoc! session-map key)
                         nil))

(defn ttl-memory-store
      "Returns an implementation of SessionStore based on an ExpiringMap"
      ([] (ExpiringMapStore. session-map)))

(defn keep-alive
      "Given a UID, keep it alive."
      [key]
      (debugf "keep-alive: %s" key)
      (em/reset-expiration! session-map key)
      )

(defn add-token
      "Given a UID and a token, remember it."
      [key token]
      (debugf "(add-token) %s: %s" key token)

      (let [session (assoc (get session-map key) :sente-token token)]
           (em/assoc! session-map key session)))

(defn get-token
      "Given a UID, retrieve the associated token, if any."
      [uid]
      (debugf "get-token" uid)
      (let [session (get session-map key)]
           (get session :sente-token)))

(defn session-uid
      "Convenient to extract the UID that Sente needs from the request."
      [ring-req]
      (:session/key ring-req))

(defn session-role
      "Convenient to extract the users role from a ring-request."
      [ring-req]
      (let [friend-identity (get-in ring-req [:session :cemerick.friend/identity] nil)
            current (if friend-identity (:current friend-identity) nil)
            roles (if current (get-in friend-identity [:authentications current :roles] nil))
            role (if roles (first roles) "anonymous")]
           role)
      )