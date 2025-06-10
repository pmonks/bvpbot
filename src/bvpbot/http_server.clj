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
  <head><title>bvpbot status</title></head>
  <body><p style='font-family:sans-serif'>bvpbot Discord bot up for " (u/runtime-info) ".<p></body>
</html>")})

(defstate http-status-port
  :start (if-let [port (:http-status-port cfg/config)] port 8080))

(defstate http-status-handler-server
  :start (do
           (log/info (str "Starting HTTP status server on port " http-status-port))
           (http/run-server http-status-handler {:port http-status-port :legacy-return-value? false}))
  :stop  (when-let [stopping (http/server-stop! http-status-handler-server)] @stopping))
