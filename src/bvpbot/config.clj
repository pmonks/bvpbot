;
; Copyright © 2020 Peter Monks
;
; Licensed under the Apache License, Version 2.0 (the "License");
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
;     http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.
;
; SPDX-License-Identifier: Apache-2.0
;

(ns bvpbot.config
  (:require [clojure.java.io        :as io]
            [clojure.string         :as s]
            [clojure.edn            :as edn]
            [java-time              :as tm]
            [aero.core              :as a]
            [mount.core             :as mnt :refer [defstate]]
            [org.httpkit.client     :as http]
            [org.httpkit.sni-client :as sni-client]
            [bvpbot.util            :as u]))

; Because java.util.logging is a hot mess
(org.slf4j.bridge.SLF4JBridgeHandler/removeHandlersForRootLogger)
(org.slf4j.bridge.SLF4JBridgeHandler/install)

; Because Java's default exception behaviour in threads other than main is a hot mess
(Thread/setDefaultUncaughtExceptionHandler
 (reify Thread$UncaughtExceptionHandler
   (uncaughtException [_ t e]
     (u/log-exception e (str "Uncaught exception in thread " (.getName t))))))

; See https://github.com/http-kit/http-kit#enabling-client-sni-support-disabled-by-default
(alter-var-root #'http/*default-client* (fn [_] sni-client/default-client))

; Adds a #split reader macro to aero - see https://github.com/juxt/aero/issues/55
(defmethod a/reader 'split
  [_ _ value]
  (let [[s re] value]
    (when (and s re)
      (s/split s (re-pattern re)))))

(defmulti ^:private trim-config-value
  class)

(defmethod ^:private trim-config-value java.lang.String
  [x]
  (when (not (s/blank? x))
    (s/trim x)))

(defmethod ^:private trim-config-value :default
  [x]
  x)

(defn ensure-config-value
  [m k]
  (if-let [result (trim-config-value (get m k))]
    result
    (throw (ex-info (str "Config key '"(name k)"' not provided") {}))))


(defstate config
  :start (if-let [config-file (:config-file (mnt/args))]
           (a/read-config config-file)
           (a/read-config (io/resource "config.edn"))))

; Note: do NOT use mount for these, since they're used before mount has started
(def build-config
  (if-let [deploy-info (io/resource "deploy-info.edn")]
    (edn/read-string (slurp deploy-info))
    (throw (RuntimeException. "deploy-info.edn classpath resource not found."))))

(def git-tag
  (when (:tag build-config)
    (s/trim (:tag build-config))))

(def git-url
  (when-not (s/blank? git-tag)
    (str "https://github.com/pmonks/bvpbot/tree/" git-tag)))

(def built-at
  (tm/instant (:date build-config)))

(def build-info (str (tm/format :iso-instant (or (:build-date build-config) (tm/instant)))
                     (when (:repo build-info)
                       (str " from [" (if-let [tag (:tag build-config)] tag (:sha build-config)) "](" (:build-url build-config) ")"))))

(defn set-log-level!
  "Sets the log level (which can be a string or a keyword) of the bot, for the given logger aka 'package' (a String, use 'ROOT' for the root logger)."
  [level ^String logger-name]
  (when (and level logger-name)
    (let [logger    ^ch.qos.logback.classic.Logger (org.slf4j.LoggerFactory/getLogger logger-name)                       ; This will always return a Logger object, even if it isn't used
          level-obj                                (ch.qos.logback.classic.Level/toLevel (s/upper-case (name level)))]   ; Note: this code defaults to DEBUG if the given level string isn't valid
      (.setLevel logger level-obj))))

(defn reset-logging!
  "Resets all log levels to their configured defaults."
  []
  (let [lc ^ch.qos.logback.classic.LoggerContext (org.slf4j.LoggerFactory/getILoggerFactory)
        ci (ch.qos.logback.classic.util.ContextInitializer. lc)]
    (.reset lc)
    (.autoConfig ci)))
