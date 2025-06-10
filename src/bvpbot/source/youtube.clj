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

(def api-host                    "https://www.googleapis.com")
(def endpoint-get-channel-info   "/youtube/v3/channels?part=snippet&id=%s")
(def endpoint-get-channel-videos "/youtube/v3/search?part=snippet&order=date&type=video&maxResults=50&channelId=%s")

(defn google-api-call
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

(defn- sanitise-video-data-structure
  "Sanitise (as in 'make sane'...) the herpaderp YouTube API video data structure. 🙄"
  [items]
  (seq (keep identity (map #(assoc (:snippet %) :id (:video-id (:id %))) items))))

(defn- remove-premieres
  "Removes 'premieres' (empty placeholders) from the given sequence of YouTube videos, because the herpaderp YouTube APIs don't support this directly. 🙄"   ; See https://stackoverflow.com/a/66596615/369849
  [videos]
  (remove #(= "upcoming" (:live-broadcast-content %)) videos))

(defn videos
  "Retrieves up to 50 videos for the given YouTube channel (or nil if there aren't any) newest first, optionally limited to those published since the given date."
  ([youtube-api-token channel-id] (videos youtube-api-token nil channel-id))
  ([youtube-api-token since channel-id]
    (remove-premieres
      (let [endpoint (if since
                       (format (str endpoint-get-channel-videos "&publishedAfter=%s") channel-id (str (tm/instant since)))
                       (format endpoint-get-channel-videos channel-id))]
        (sanitise-video-data-structure (:items (google-api-call youtube-api-token endpoint)))))))

(defn all-videos
  "Retrieves all videos for the given YouTube channel, newest first.  Use with caution: this can be very expensive from a quota unit perspective (100 units / 50 videos)."
  [youtube-api-token channel-id]
  (remove-premieres
    (loop [endpoint     (format endpoint-get-channel-videos channel-id)
           publish-date (tm/instant)
           result       []]
      (let [items            (sanitise-video-data-structure (:items (google-api-call youtube-api-token endpoint)))
            new-publish-date (:published-at (last items))]
        (if (and (not (s/blank? new-publish-date))
                 (tm/before? (tm/instant new-publish-date) (tm/instant publish-date)))
          (recur (format (str endpoint-get-channel-videos "&publishedBefore=%s") channel-id new-publish-date)
                 new-publish-date
                 (into result items))
          (into result items))))))
