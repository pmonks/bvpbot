;
; Copyright © 2020 Peter Monks
;
; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at https://mozilla.org/MPL/2.0/.
;
; SPDX-License-Identifier: MPL-2.0
;

(ns bvpbot.source.ist
  (:require [clojure.string     :as s]
            [clojure.java.io    :as io]
            [clojure.edn        :as edn]
            [markov-chains.core :as mc]
            [bvpbot.util        :as u]))

(def markov-chain (if-let [ist-markov-chain (io/resource "ist-markov-chain.edn")]
                    (edn/read-string (slurp ist-markov-chain))
                    (throw (RuntimeException. "ist-markov-chain classpath resource not found - did you remember to run the 'gen-ist-markov' alias first?"))))

(defn gen-title
  ([] (gen-title markov-chain))
  ([chain]
   (u/replace-all (s/join " "
                          (take 100   ; Make sure we eventually drop out
                                (take-while (partial not= "🔚")
                                            (drop-while #(or (= "🔚" %) (re-matches #"(\p{Punct})+" %))   ; Drop leading title breaks and punctuation
                                                        (mc/generate chain)))))
                  [[#"\s+([!?:;,\"…\*\.])" "$1"]])))  ; Collapse whitespace before punctuation
