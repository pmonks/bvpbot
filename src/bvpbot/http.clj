;
; Copyright © 2022 Peter Monks
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

(ns bvpbot.http
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
