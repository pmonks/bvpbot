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

(ns bvpbot.discord.connection
  (:require [clojure.core.async          :as a]
            [clojure.tools.logging       :as log]
            [mount.core                  :as mnt :refer [defstate]]
            [discljord.messaging         :as dmsg]
            [discljord.connections       :as dconn]
            [discljord.events            :as de]
            [slash.core                  :as sc]
            [slash.gateway               :as sg]
            [embroidery.api              :as e]
            [bvpbot.config               :as cfg]
            [bvpbot.util                 :as u]
            [bvpbot.discord.commands     :as cmd]
            [bvpbot.discord.message-util :as mu]))

(declare api-token)  ; Hokey hack to appease clj-kondo
(defstate api-token
  :start (cfg/ensure-config-value cfg/config :discord-api-token))

(declare rest-conn)  ; Hokey hack to appease clj-kondo
(defstate rest-conn
  :start (dmsg/start-connection! api-token)
  :stop  (dmsg/stop-connection! rest-conn))

(declare event-channel)  ; Hokey hack to appease clj-kondo
(defstate event-channel
  :start (a/chan (get cfg/config :discord-event-channel-size 100))
  :stop  (a/close! event-channel))

(defstate gateway-conn
  :start (dconn/connect-bot! api-token event-channel :intents #{})
  :stop  (dconn/disconnect-bot! gateway-conn))

(declare application-id)  ; Hokey hack to appease clj-kondo
(defstate application-id
  :start (:id (mu/get-current-application-information! rest-conn)))

(defstate command-registration
  :start (if (u/safe-parse-boolean (:reload-command-definitions cfg/config))
           (do
             (log/info "Overwriting command definitions...")
             (mu/bulk-overwrite-global-application-commands! rest-conn application-id cmd/command-definitions))
           (log/info "Skipped overwriting of command definitions")))

(def ^:private interaction-create-handlers
  (assoc sg/gateway-defaults
         :application-command cmd/command-paths
         ; Other interaction sub-types (:application-command-autocomplete, etc.) would go here
         ))

(defn- interaction-create-dispatcher
  ":interaction-create event dispatcher"
  [_event-type event-data]
  (when-let [handler-result (sc/route-interaction interaction-create-handlers event-data)]
    (mu/create-interaction-response! rest-conn (:id event-data) (:token event-data) (:type handler-result) :data (:data handler-result))))

(def ^:private event-handlers
  {:interaction-create [interaction-create-dispatcher]
   ; Other Discord events (:message-create, etc.) would go here
  })

(defn- event-dispatcher
  "Global event dispatcher"
  [event-type event-data]
  (e/future*
    (try
      (de/dispatch-handlers event-handlers event-type event-data)
      (catch Exception e
        (u/log-exception e "Unhandled exception in event dispatcher")))))

(defn start-message-pump!
  "Start the Discord message pump.

  Note: blocks the calling thread until the event-channel is closed."
  []
  (de/message-pump! event-channel event-dispatcher))
