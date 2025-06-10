;
; Copyright © 2024 Peter Monks
;
; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at https://mozilla.org/MPL/2.0/.
;
; SPDX-License-Identifier: MPL-2.0
;

(ns bvpbot.source.urban-dictionary
  (:require [clojure.string        :as s]
            [clojure.tools.logging :as log]
            [bvpbot.http-client    :as hc]
            [bvpbot.util           :as u]))

(def ^:private api-location           "https://api.urbandictionary.com/v0")
(def ^:private define-term-url-format (str api-location "/define?term=%s"))

(defn define-term
  "Defines the given term (a String) as per Urban Dictionary, returning a
  Clojure data structure.

  Throws on I/O or JSON parsing errors."
  [^String term]
  (when-not (s/blank? term)
      (let [api-url                     (format define-term-url-format term)
            headers                     {"Accept" "application/json"}
            _                           (log/debug "Calling" api-url)
            {:keys [status body error]} (hc/get api-url headers)]
        (if (or error (not= status 200))
          (throw (ex-info (format "Urban Dictionary API call (%s) failed" api-url) {:status status :body body} error))
          (u/parse-json body)))))

(defn top-definition-for-term
  "Returns the 'top' Urban Dictionary definition for the given term.

  Note: Urban Dictionary doesn't return items sorted in any known order, so we
  have to 'score' the results ourselves.  We choose to use Lidstone smoothing."
  [term]
  (when-let [definitions (define-term term)]
    (last (sort-by #(u/lidstone-scoring (:thumbs-up %) (:thumbs-down %)) (:list definitions)))))

(defn to-plain-text
  "Converts the given Urban Dictionary definition text to plain text (i.e.
  strips out formatting characters)."
  [s]
  (when s
    (-> s
        (s/replace "[" "")
        (s/replace "]" ""))))