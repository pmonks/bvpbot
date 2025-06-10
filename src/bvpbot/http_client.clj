;
; Copyright © 2024 Peter Monks
;
; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at https://mozilla.org/MPL/2.0/.
;
; SPDX-License-Identifier: MPL-2.0
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
