;
; Copyright © 2020 Peter Monks
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

(ns bvpbot.discord.message-util
  (:require [clojure.string        :as s]
            [clojure.tools.logging :as log]
            [java-time             :as tm]
            [discljord.messaging   :as dm]
            [discljord.formatting  :as df]))

(defn- discljord-deref
  "discljord defaults to 'fire & forget' API calls, which silently swallows errors. This corrects that behaviour."
  [discljord-api-result]
  (if (instance? java.lang.Throwable @discljord-api-result)
    (throw @discljord-api-result)
    @discljord-api-result))

(def embed-template-colour   9215480)
(def embed-template-logo-url "https://raw.githubusercontent.com/pmonks/bvpbot/dev/bvpbot.png")

(defn embed-template-no-footer
  "Generates a default template for embeds, without a footer."
  []
  {:color embed-template-colour})

(defn discord-escape
  "Escapes s (a String) for use in a Discord message or embed."
  [s]
  (when s
    (s/escape s {\* "\\*"
                 \` "\\`"
                 \~ "\\~"
                 \_ "\\_"
                 \< "\\<"
                 \> "\\>"})))

(defn embed-template
  "Generates a default template for embeds."
  []
  (merge (embed-template-no-footer)
         {:footer    {:text "bvpbot"
                      :icon_url embed-template-logo-url}
          :timestamp (str (tm/instant))}))

(defn get-current-application-information!
  "A version of get-current-application-information! that throws errors."
  [discord-message-channel]
  (discljord-deref (dm/get-current-application-information! discord-message-channel)))

(defn create-message!
  "A version of discljord.messaging/create-message! that throws errors."
  [discord-message-channel channel-id & args]
  (log/debug "Sending message to Discord channel" (str channel-id " with args:") args)
  (discljord-deref (apply dm/create-message! discord-message-channel channel-id args)))

(defn edit-message!
  "A version of discljord.messaging/edit-message! that throws errors."
  [discord-message-channel channel-id message-id & args]
  (log/debug "Editing message" message-id "in Discord channel" (str channel-id " with args:") args)
  (discljord-deref (apply dm/edit-message! discord-message-channel channel-id message-id args)))

(defn delete-message!
  "A version of discljord.messaging/delete-message! that throws errors."
  [discord-message-channel channel-id message-id]
  (log/debug "Deleting message-id" message-id)
  (discljord-deref (dm/delete-message! discord-message-channel channel-id message-id)))

(defn create-reaction!
  "A version of discljord.messaging/create-reaction! that throws errors. Note: for custom emoji, use the name and snowflake of the emoji, but not the <> tag delimiters (e.g. YC:698349911028269078)."
  [discord-message-channel channel-id message-id reaction]
  (log/debug "Adding reaction" reaction "to message-id" message-id)
  (discljord-deref (dm/create-reaction! discord-message-channel channel-id message-id reaction)))

(defn create-dm!
  "A version of discljord.messaging/create-dm! that throws errors."
  [discord-message-channel user-id]
  (log/debug "Creating DM channel with user-id" user-id)
  (discljord-deref (dm/create-dm! discord-message-channel user-id)))

(defn get-channel!
  "A version of discljord.messaging/get-channel! that throws errors."
  [discord-message-channel channel-id]
  (log/debug "Obtaining channel information for channel" channel-id)
  (discljord-deref (dm/get-channel! discord-message-channel channel-id)))

(defn send-dm!
  "Convenience method that creates a DM channel to the specified user and sends the given message to them."
  [discord-message-channel user-id message]
  (let [dm-channel (create-dm! discord-message-channel user-id)
        channel-id (:id dm-channel)]
    (create-message! discord-message-channel channel-id :content message)))

(defn bulk-overwrite-global-application-commands!
  "A version of discljord.messaging/bulk-overwrite-guild-application-commands! that throws errors."
  [discord-message-channel application-id application-commands]
  (discljord-deref (dm/bulk-overwrite-global-application-commands! discord-message-channel
                                                                   application-id
                                                                   application-commands)))

(defn create-interaction-response!
  "A version of discljord.messaging/create-interaction-response! that throws errors."
  [discord-message-channel interaction-id interaction-token response-type & args]
  (discljord-deref (apply dm/create-interaction-response! discord-message-channel
                                                          interaction-id
                                                          interaction-token
                                                          response-type  ; https://discord.com/developers/docs/interactions/receiving-and-responding#interaction-response-object-interaction-callback-type
                                                          args)))

(defn delete-original-interaction-response!
  "A version of discljord.messaging/delete-original-interaction-response! that throws errors."
  [discord-message-channel application-id interaction-token & args]
  (discljord-deref (apply dm/delete-original-interaction-response! discord-message-channel
                                                                   application-id
                                                                   interaction-token
                                                                   args)))

(defn direct-message?
  "Was the given event sent via a Direct Message?"
  [event-data]
  (not (:guild-id event-data)))  ; Direct messages don't have a :guild-id in their event data

(defn bot-message?
  "Was the given event generated by a bot?"
  [event-data]
  (:bot (:author event-data)))

(defn human-message?
  "Was the given event generated by a human?"
  [event-data]
  (not (bot-message? event-data)))

(defn nick-or-user-name
  "Convenience method that returns the nickname, or (if there isn't one) username, of the author of the given message."
  [event-data]
  (if-let [name (:nick (:member event-data))]
    name
    (:username (:author event-data))))

(defn channel-link
  "Convenience method that creates a link to the given channel-id, for embedding in message bodies"
  [channel-id]
  (when channel-id
    (df/mention-channel channel-id)))

(defn user-link
  "Convenience method that creates a link to the given user-id, for embedding in message bodies"
  [user-id]
  (when user-id
    (df/mention-user user-id)))

(defn message-url
  "Convenience method that creates a URL to a specific message"
  [guild-id channel-id message-id]
  (when (and guild-id channel-id message-id)
    (str "https://discord.com/channels/" guild-id "/" channel-id "/" message-id)))

(defmulti timestamp-tag
  "Convenience method that creates a Discord 'dynamic timestamp' tag for the provided date value and (optional) Discord format specifier (a character; one of \\d \\f \\t \\D \\F \\R \\T - see https://www.reddit.com/r/discordapp/comments/ohmxrc/new_inline_dynamic_timestamps_that_anyone_can_type/ for examples)."
  (fn [i & _] (class i)))

(defmethod timestamp-tag Long
  ([i]        (timestamp-tag i nil))
  ([i format] (str "<t:" i (when format (str ":" format)) ">")))

(defmethod timestamp-tag java.time.Instant
  ([^java.time.Instant i]        (timestamp-tag i nil))
  ([^java.time.Instant i format] (timestamp-tag (.getEpochSecond i) format)))

(defmethod timestamp-tag java.util.Date
  ([^java.util.Date i]        (timestamp-tag i nil))
  ([^java.util.Date i format] (timestamp-tag (long (/ (.getTime i) 1000)) format)))

(defmethod timestamp-tag java.time.ZonedDateTime
  ([^java.time.ZonedDateTime i]        (timestamp-tag i nil))
  ([^java.time.ZonedDateTime i format] (timestamp-tag (.toEpochSecond i) format)))
