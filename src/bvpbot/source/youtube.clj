;
; Copyright © 2020 Peter Monks
;
; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at https://mozilla.org/MPL/2.0/.
;
; SPDX-License-Identifier: MPL-2.0
;

(ns bvpbot.source.youtube
  (:require [clojure.string         :as s]
            [clojure.tools.logging  :as log]
            [java-time              :as tm]
            [bvpbot.http-client     :as hc]
            [bvpbot.util            :as u]))

(def ^:private api-host                    "https://www.googleapis.com")
(def ^:private endpoint-get-channel-info   "/youtube/v3/channels?part=snippet&id=%s")
(def ^:private endpoint-get-playlist-items "/youtube/v3/playlistItems?part=snippet&maxResults=50&playlistId=%s")

(defn- google-api-call
  "Calls the given Google API endpoint (must be fully constructed), using the provided API key, and either returns parsed hashmap of the body or throws an ex-info."
  [youtube-api-token endpoint]
  (let [api-url                     (str api-host endpoint)
        headers                     {"Accept" "application/json"}
        _                           (log/debug "Calling" (str api-url "&key=REDACTED"))
        {:keys [status body error]} (hc/get (str api-url "&key=" youtube-api-token) headers)]
    (if (or error (not= status 200))
      (throw (ex-info (format "Google API call (%s) failed" (str api-url "&key=REDACTED")) {:status status :body body} error))
      (u/parse-json body))))

(defn channel-info
  "Returns info for the given channel, or throws an ex-info."
  [youtube-api-token channel-id]
  (:snippet (first (:items (google-api-call youtube-api-token (format endpoint-get-channel-info channel-id))))))

(defn channel-id->playlist-id
  "Turns a YouTube channel id into a playlist id for the default playlist in
  that channel."
  [channel-id]
  (when-not (s/blank? channel-id)
    (str "UU" (subs channel-id 2))))

(defn playlist-page
  "Retrieves a page (up to 50) of playlist items from `playlist-id`, identified
  by `page-token` (the latest page if not specified), or `nil` if there aren't
  any. Results are reverse sorted by publish date."
  ([youtube-api-token playlist-id] (playlist-page youtube-api-token nil playlist-id))
  ([youtube-api-token page-token playlist-id]
    (let [endpoint (if (s/blank? page-token)
                     (format endpoint-get-playlist-items playlist-id)
                     (format (str endpoint-get-playlist-items "&pageToken=%s") playlist-id page-token))]
      (google-api-call youtube-api-token endpoint))))

(defn- safe-instant
  "Because java-time.api/instant isn't nil safe."
  [x]
  (when x
    (tm/instant x)))

(defn- include-item?
  "Should the given item be included in the result?"
  [since item]
  (if since
    (let [published-at (safe-instant (:published-at (:snippet item)))]
      (and published-at (tm/< since published-at)))
    true))

(defn all-playlist-items
  "Retrieves all videos for the given YouTube playlist, newest first, back as
  far as `since` (or all if it's `nil`)."
  ([youtube-api-token playlist-id] (all-playlist-items youtube-api-token nil playlist-id))
  ([youtube-api-token since playlist-id]
   (let [since (when since (safe-instant since))]
     (loop [current-page (playlist-page youtube-api-token playlist-id)
            page-token   (:next-page-token current-page)
            result       (filter (partial include-item? since) (:items current-page))]  ; Only add items that are newer than since
       (let [last-published-at (safe-instant (:published-at (:snippet (last (:items current-page)))))]
         (if (or (not page-token)
                 (and since last-published-at (tm/> since last-published-at)))
           (seq result)
           (let [next-page (playlist-page youtube-api-token page-token playlist-id)]
             (recur next-page
                    (:next-page-token next-page)
                    (concat result (filter (partial include-item? since) (:items next-page)))))))))))
