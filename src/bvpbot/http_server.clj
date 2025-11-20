;
; Copyright © 2022 Peter Monks
;
; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at https://mozilla.org/MPL/2.0/.
;
; SPDX-License-Identifier: MPL-2.0
;

(ns bvpbot.http-server
  (:require [clojure.tools.logging :as log]
            [mount.core            :as mnt :refer [defstate]]
            [org.httpkit.server    :as http]
            [bvpbot.config         :as cfg]
            [bvpbot.util           :as u]))

(defn- http-status-handler
  [_]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    (str "<!DOCTYPE html>
<html>
  <head>
    <style type=\"text/css\">
      html{
        max-width: 70ch;
        padding: 1em;
        margin: auto;
        line-height: 1.75;
        font-size: 2.25em;
        font-family: sans-serif;
      }
      p,ul,ol {
        margin-bottom: 2em;
      }
      img{
        max-width: 100%;
        height: auto;
      }
    </style>
    <title>bvpbot status</title>
  </head>
  <body>
    <h1>bvpbot status</h1>
    <p>Up for: " (u/runtime-info) "<br/>
    OS: " u/os-info "<br/>
    JVM: " u/jvm-info "<br/>
    Clojure: " u/clojure-info "<br/>
    Heap: " (u/heap-mem-info) "<br/>
    Non-heap: " (u/non-heap-mem-info) "</p>
  </body>
</html>")})

(defstate http-status-port
  :start (if-let [port (:http-status-port cfg/config)] port 8080))

(defstate http-status-handler-server
  :start (do
           (log/info (str "Starting HTTP status server on port " http-status-port))
           (http/run-server http-status-handler {:port http-status-port :legacy-return-value? false}))
  :stop  (when-let [stopping (http/server-stop! http-status-handler-server)] @stopping))
