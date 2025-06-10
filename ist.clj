;
; Copyright © 2021 Peter Monks
;
; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at https://mozilla.org/MPL/2.0/.
;
; SPDX-License-Identifier: MPL-2.0
;

(ns ist
  "IST script for bvpbot.

  To use:

    clojure -T:ist <task-name> <task-parameters>

  For more information, run:

    clojure -A:deps -T:ist help/doc"
  (:require [bvpbot.ist.main :as ist]))

(defn generate-markov
  "(Re)generate the IST Markov chain."
  [opts]
  (if-let [youtube-api-key (:youtube-api-key opts)]
    (ist/-main youtube-api-key)
    (throw (ex-info ":youtube-api-key missing from tool invocation" (into {} opts)))))
