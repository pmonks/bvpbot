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

(def ist-command
  (scs/command
   "ist"
   "Generates a fake IST video title"
   :options []))

(defhandler ist-handler
  ["ist"]
  _interaction
  [_]
  (rsp/channel-message {:embeds [(assoc (mu/embed-template)
                                        :description (str "**" (mu/discord-escape (ist/gen-title)) "**")
                                        :footer      {:text     "Disclaimer: this is a generated fake"
                                                      :icon_url "https://yt3.ggpht.com/ytc/AAUvwnjhzwc9yNfyfX8C1N820yMhaS27baWlSz2wqaRE=s176-c-k-c0x00ffffff-no-rj"})]}))

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
    (rsp/channel-message {:embeds [(assoc (mu/embed-template)
                                          :description (str "**Top Definition of [`" term "` on Urban Dictionary](https://www.urbandictionary.com/define.php?term=" term "):**\n\n"
                                                            "`" (:definition definition) "`"
                                                            (when-let [example (:example definition)]
                                                              (str "\n\n**Example usage:**\n\n_" example "_"))))]})
    (rsp/ephemeral
      (rsp/channel-message {:embeds [(assoc (mu/embed-template)
                                            :description (str "No definition was found on Urban Dictionary for `" term "`."))]}))))

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
    (rsp/channel-message {:embeds [(assoc (mu/embed-template)
                                          :description "[bvpbot's privacy policy is available here](https://github.com/pmonks/bvpbot/blob/release/PRIVACY.md)")]})))

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
    (rsp/channel-message {:embeds [(assoc (mu/embed-template)
                                          :fields [
                                            {:name "Running for"            :value (mu/discord-escape (u/runtime-info))}
                                            {:name "Built at"               :value (mu/discord-escape cfg/build-info)}
                                            {:name "Clojure"                :value (mu/discord-escape u/clojure-info)}
                                            {:name "JVM"                    :value (mu/discord-escape u/jvm-info)}
                                            {:name "OS"                     :value (mu/discord-escape u/os-info)}
                                            {:name "Heap memory"            :value (mu/discord-escape (u/heap-mem-info))}
                                            {:name "Non-heap memory"        :value (mu/discord-escape (u/non-heap-mem-info))}
                                          ])]})))

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
    (rsp/channel-message {:embeds [(assoc (mu/embed-template)
                                          :fields (map #(hash-map :name (str "/" (:name %)) :value (mu/discord-escape (:description %)))
                                                       command-definitions))]})))

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
