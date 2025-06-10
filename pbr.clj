;
; Copyright © 2024 Peter Monks
;
; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at https://mozilla.org/MPL/2.0/.
;
; SPDX-License-Identifier: MPL-2.0
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
                            :licenses         [:license   {:name "MPL-2.0" :url "https://www.mozilla.org/en-US/MPL/2.0/"}]
                            :developers       [:developer {:id "pmonks" :name "Peter Monks" :email "pmonks+bvpbot@gmail.com"}]
                            :scm              {:url "https://github.com/pmonks/bvpbot" :connection "scm:git:git://github.com/pmonks/bvpbot.git" :developer-connection "scm:git:ssh://git@github.com/pmonks/bvpbot.git"}
                            :issue-management {:system "github" :url "https://github.com/pmonks/bvpbot/issues"}}
         :antq             {:transitive false}))
