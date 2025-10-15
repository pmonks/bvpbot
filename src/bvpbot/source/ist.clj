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
            [markov-chains.core :as mc]))

(def ^:private markov-chain (if-let [ist-markov-chain (io/resource "ist-markov-chain.edn")]
                              (edn/read-string (slurp ist-markov-chain))
                              (throw (RuntimeException. "ist-markov-chain classpath resource not found - did you remember to run the 'gen-ist-markov' alias first?"))))

(def ^:private min-title-length 2)
(def ^:private max-title-length 100)

(defn- gen-up-to-tokens
  "Generates no more than `mx` tokens from Markov chain `chain`."
  [chain mx]
  (take mx
        (take-while (partial not= "🔚")
                    (drop-while #(or (= "🔚" %) (re-matches #"(\p{Punct})+" %))   ; Drop leading title breaks and punctuation
                                (mc/generate chain)))))

(defn- gen-between-tokens
  "Generates between `mn` and `mx` tokens from Markov chain `chain`."
  [chain mn mx]
  (loop [tokens (gen-up-to-tokens chain mx)]
    (if (> (count tokens) mn)
      tokens
      (recur (gen-up-to-tokens chain mx)))))

(defn gen-title
  ([] (gen-title markov-chain min-title-length max-title-length))
  ([chain mn mx]
   (-> (s/join " " (gen-between-tokens chain mn mx))
       (s/replace #"\s+([!?:;,\"…\*\.])" "$1")     ; Collapse whitespace before punctuation
       (s/replace #"\s+'\s*s\s+"         "'s ")    ; Collapse orphaned plurals
       (s/replace #"\s+'\s*S\s+"         "'S ")    ;    "         "      "
       (s/replace #"\s+\(\s+"            " (")     ; Collapse orphaned parens
       (s/replace #"\s+\)\s+"            ") "))))  ;    "         "      "
