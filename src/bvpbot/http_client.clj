;
; Copyright © 2024 Peter Monks
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

(ns bvpbot.http-client
  (:refer-clojure :exclude [get])
  (:require [clojure.string :as s]
            [hato.client    :as hc]))

(def ^:private http-client-d (delay (hc/build-http-client {:connect-timeout 1000
                                                           :redirect-policy :normal
                                                           :cookie-policy   :none})))

(defn get
  "Attempts an HTTP GET on the given URI. Returns the raw responsefor further
  processing by the caller.

  Throws on I/O exceptions."
  ([uri] (get uri nil))
  ([uri headers]
   (when-not (s/blank? uri)
     (hc/get uri
             {:http-client @http-client-d
              :header      (merge {"user agent" "com.github.pmonks/bvpbot"}
                                  headers)}))))
