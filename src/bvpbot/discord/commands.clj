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

(ns bvpbot.discord.commands
  (:require [slash.command                  :as scmd :refer [defhandler defpaths]]
            [slash.command.structure        :as scs]
            [slash.response                 :as rsp]
            [bvpbot.config                  :as cfg]
            [bvpbot.util                    :as u]
            [bvpbot.discord.message-util    :as mu]
            [bvpbot.source.ist              :as ist]
            [bvpbot.source.urban-dictionary :as ud]))

(defn- embed
  "Construct an embed message with the given args."
  [& args]
  {:embeds [(apply assoc (mu/embed-template) args)]})

(def ist-command
  (scs/command
   "ist"
   "Generates a fake IST video title"
   :options []))

(defhandler ist-handler
  ["ist"]
  _interaction
  [_]
  (let [fake-ist-title (ist/gen-title)]
    (rsp/channel-message (embed :description (mu/discord-trim :embed-description (str "**" (mu/discord-escape fake-ist-title) "**"))
                                :footer      {:text     "Disclaimer: this is a generated fake"
                                              :icon_url "https://yt3.ggpht.com/ytc/AAUvwnjhzwc9yNfyfX8C1N820yMhaS27baWlSz2wqaRE=s176-c-k-c0x00ffffff-no-rj"}))))

(def ud-command
  (scs/command
    "ud"
    "Looks up the Urban Dictionary definition of a term or phrase"
    :options [(scs/option "term" "The term or phrase to look up" :string :required true)]))

(defhandler ud-handler
  ["ud"]
  _interaction
  {:keys [term]}
  (if-let [definition (ud/top-definition-for-term term)]
    (let [definition-text (ud/definition-to-plain-text (:definition definition))
          definition-text (if (> (count definition-text) 3072) (str (subs definition-text 0 3069) "...") definition-text)
          example-text    (:example definition)
          example-text    (if (> (count example-text) 512) (str (subs example-text 0 509) "...") example-text)]
      (rsp/channel-message (embed :description (str "**Top Definition of [`" (mu/discord-escape term) "`](https://www.urbandictionary.com/define.php?term=" (u/query-string-escape term) ") on Urban Dictionary:**\n\n"
                                                    "`" (mu/discord-escape definition-text) "`"
                                                    (when example-text
                                                      (str "\n\n**Example usage:**\n\n_" (mu/discord-escape example-text) "_"))))))
    (rsp/channel-message (embed :description (str "No definition was found on Urban Dictionary for `" (mu/discord-escape term) "`.")))))

(def privacy-command
  (scs/command
    "privacy"
    "Provides a link to bvpbot's privacy policy"
    :options []))

(defhandler privacy-handler
  ["privacy"]
  _interaction
  [_]
  (rsp/ephemeral
    (rsp/channel-message (embed :description "[bvpbot's privacy policy is available here](https://github.com/pmonks/bvpbot/blob/release/PRIVACY.md)"))))

(def status-command
  (scs/command
    "status"
    "Provides bvpbot's technical status"
    :options []))

(defhandler status-handler
  ["status"]
  _interaction
  [_]
  (rsp/ephemeral
    (rsp/channel-message (embed :fields [
                                  {:name "Running for"     :value (mu/discord-trim :embed-field-value (mu/discord-escape (u/runtime-info)))}
                                  {:name "Built at"        :value (mu/discord-trim :embed-field-value (mu/discord-escape cfg/build-info))}
                                  {:name "Clojure"         :value (mu/discord-trim :embed-field-value (mu/discord-escape u/clojure-info))}
                                  {:name "JVM"             :value (mu/discord-trim :embed-field-value (mu/discord-escape u/jvm-info))}
                                  {:name "OS"              :value (mu/discord-trim :embed-field-value (mu/discord-escape u/os-info))}
                                  {:name "Heap memory"     :value (mu/discord-trim :embed-field-value (mu/discord-escape (u/heap-mem-info)))}
                                  {:name "Non-heap memory" :value (mu/discord-trim :embed-field-value (mu/discord-escape (u/non-heap-mem-info)))}
                                ]))))

(def help-command
  (scs/command
    "help"
    "Provides brief help on bvpbot's supported commands"
    :options []))

(declare command-definitions)

(defhandler help-handler
  ["help"]
  _interaction
  [_]
  (rsp/ephemeral
    (rsp/channel-message (embed :fields (map #(hash-map :name (mu/discord-trim :embed-field-name (str "/" (:name %))) :value (mu/discord-trim :embed-field-value (mu/discord-escape (:description %)))) command-definitions)))))

(def command-definitions [
  ist-command
  ud-command
  privacy-command
  status-command
  help-command])

(defpaths command-paths
  #'ist-handler
  #'ud-handler
  #'status-handler
  #'privacy-handler
  #'help-handler)
