;
; Copyright © 2024 Peter Monks
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

(defn set-opts
  [opts]
  (assoc opts
         :lib              'com.github.pmonks/bvpbot
         :version          (format "1.0.%s" (.format (java.text.SimpleDateFormat. "yyyyMMdd") (java.util.Date.)))
         :uber-file        "./target/bvpbot-standalone.jar"
         :main             'bvpbot.main
         :deploy-info-file "./resources/deploy-info.edn"
         :prod-branch      "release"
         :write-pom        true
         :validate-pom     true
         :pom              {:description      "A Discord bot."
                            :url              "https://github.com/pmonks/bvpbot"
                            :licenses         [:license   {:name "Apache-2.0" :url "http://www.apache.org/licenses/LICENSE-2.0.html"}]
                            :developers       [:developer {:id "pmonks" :name "Peter Monks" :email "pmonks+bvpbot@gmail.com"}]
                            :scm              {:url "https://github.com/pmonks/bvpbot" :connection "scm:git:git://github.com/pmonks/bvpbot.git" :developer-connection "scm:git:ssh://git@github.com/pmonks/bvpbot.git"}
                            :issue-management {:system "github" :url "https://github.com/pmonks/bvpbot/issues"}}
         :antq             {:transitive false}))
