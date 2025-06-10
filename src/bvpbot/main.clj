;
; Copyright © 2020 Peter Monks
;
; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at https://mozilla.org/MPL/2.0/.
;
; SPDX-License-Identifier: MPL-2.0
;

#_{:clj-kondo/ignore [:unused-namespace]}
(ns bvpbot.main
  (:require [CLJ-2253]
            [clojure.string            :as s]
            [clojure.java.io           :as io]
            [clojure.tools.cli         :as cli]
            [clojure.tools.logging     :as log]
            [mount.core                :as mnt]
            [java-time                 :as tm]
            [bvpbot.config             :as cfg]
            [bvpbot.http-server        :as hs]
            [bvpbot.util               :as u]
            [bvpbot.discord.connection :as bdc])
  (:gen-class))

(def ^:private cli-opts
  [["-c" "--config-file FILE" "Path to configuration file (defaults to 'config.edn' in the classpath)"
    :validate [#(.exists  (io/file %)) "Must exist"
               #(.isFile  (io/file %)) "Must be a file"
               #(.canRead (io/file %)) "Must be readable"]]
   ["-h" "--help"]])

(defn usage
  [options-summary]
  (s/join
    \newline
    ["Runs the bvpbot Discord bot."
     ""
     "Usage: bvpbot [options]"
     ""
     "Options:"
     options-summary
     ""]))

(defn -main
  "Runs bvpbot."
  [& args]
  (try
    (log/info (str "Starting bvpbot on Clojure " u/clojure-info ", JVM " u/jvm-info ", OS " u/os-info))
    (log/info (str "Released at " (tm/format :iso-instant cfg/built-at) (when cfg/git-url (str " from " cfg/git-url))))
    (let [{:keys [options errors summary]} (cli/parse-opts args cli-opts)]
      (cond
        (:help options) (u/exit 0 (usage summary))
        errors          (u/exit 1 (str "The following errors occurred while parsing the command line:\n\n"
                                       (s/join \newline errors))))

      ; Start the bot
      (mnt/with-args options)
      (mnt/start)
      (log/info "bvpbot started")
      (bdc/start-message-pump!))
    (catch Exception e
      (u/log-exception e)
      (u/exit -1)))
  (u/exit))
