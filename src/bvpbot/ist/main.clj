;
; Copyright © 2020 Peter Monks
;
; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at https://mozilla.org/MPL/2.0/.
;
; SPDX-License-Identifier: MPL-2.0
;

(ns bvpbot.ist.main
  (:require [clojure.string         :as s]
            [clojure.java.io        :as io]
            [clojure.pprint         :as pp]
            [clojure.edn            :as edn]
            [clojure.stacktrace     :as st]
            [java-time.api          :as tm]
            [progress.indeterminate :as pi]
            [markov-chains.core     :as mc]
            [bvpbot.util            :as u]
            [bvpbot.source.youtube  :as yt]))

(def ^:private ist-youtube-channel-id  "UCmzFaEBQlLmMTWS0IQ90tgA")
(def ^:private ist-youtube-playlist-id (yt/channel-id->playlist-id ist-youtube-channel-id))
(def ^:private title-cache-filename    "titles-cache.edn")
(def ^:private chain-filename          "resources/ist-markov-chain.edn")

(def ^:private not-blank? (complement s/blank?))

(defn tokenize
  "Convert the given titles into tokens (~= words) suitable for collation into a Markov chain."
  [titles]
  (when titles
    (let [combined-titles (-> (s/join " 🔚 " titles)
                              (s/replace "&amp;"             "&")         ; Unescape select HTML entities
                              (s/replace "&amp"              "&")         ;           "
                              (s/replace "&quot;"            "\"")        ;           "
                              (s/replace "&lt;"              "<")         ;           "
                              (s/replace "&gt;"              ">")         ;           "
                              (s/replace "&nbsp;"            " ")         ;           "
                              (s/replace "&quot;"            "\"")        ;           "
                              (s/replace "&apos;"            "'")         ;           "
                              (s/replace "&mdash;"           "-")         ;           "
                              (s/replace "&#39;"             "'")         ;           "
                              (s/replace #"https?://\S+"     "")          ; Remove HTTP URLs
                              (s/replace "’"                 "'")         ; Single quotes
                              (s/replace #"[“”]"             "\"")        ; Double quotes
                              (s/replace "…"                 "...")       ; Ellipses
                              (s/replace #"([!?:;,\"\*])"    " $1 ")      ; Place whitespace around certain punctuation
                              (s/replace #"\s&(\S)"          " & $1")     ; Collapse whitespace around &
                              (s/replace #"(\S)&\s"          "$1 & ")     ;           "
                              (s/replace #"(\D)(\.+)\s"      "$1 $2 ")    ; Place whitespace after numbers
                              (s/replace #"\s-(\S)"          " - $1"))    ; Collapse whitespace around -
          word-tokenizer  (doto (java.text.BreakIterator/getWordInstance java.util.Locale/CANADA)  ; IST is based in (English speaking) Canada
                                (.setText combined-titles))]
      (loop [start  0
             end    (.next word-tokenizer)
             result []]
        (if (= end java.text.BreakIterator/DONE)
          (seq (filter not-blank? result))
          (recur end (.next word-tokenizer) (conj result (subs combined-titles start end))))))))

(defn- gen-chain
  "Generate a Markov chain for the given video titles."
  [titles]
  (mc/collate (tokenize titles) 1))    ; 1 = extra deranged IST mode, 2 = relatively sane IST mode

(defn- load-ist-titles
  "Loads IST titles from YouTube, and caches them to disk, reusing that cache if
  it already exists.  We do this because Google's API call quotas are draconian."
  [youtube-api-token]
  (let [cache-file    (io/file title-cache-filename)
        cache-data    (when (.exists cache-file) (edn/read-string (slurp cache-file)))
        cached-titles (:titles cache-data)
        since         (when-let [last-updated (:last-updated cache-data)] (tm/instant last-updated))
        _             (if cache-data
                        (pi/print (str "\nFound cache file with " (count cached-titles) " titles" (when since (str "; last updated " since))))
                        (pi/print "\nNo cache file found; reading all titles from YouTube..."))
        new-videos    (yt/all-playlist-items youtube-api-token since ist-youtube-playlist-id)
        new-titles    (map #(:title (:snippet %)) new-videos)
        _             (pi/print (str "\nFound " (count new-titles) " new titles on YouTube"))
        all-titles    (vec (distinct (concat cached-titles new-titles)))]
    (with-open [w (io/writer cache-file)]
      (pp/pprint {:last-updated (java.util.Date.)  ; See https://clojure.atlassian.net/browse/CLJ-2224 for why we can't use an instant here 🙄
                  :titles       all-titles}
                 w))
    all-titles))

(defn- load-bonus-titles
  []
  (when-let [bonus-file (io/resource "bonus-titles.edn")]
    (println "bonus-titles.edn found, reading titles...")
    (edn/read-string (slurp bonus-file))))

(defn -main
  [& args]
  (try
    (when (not= 1 (count args))
      (u/exit -1 "Please provide a YouTube API key on the command line."))

    (let [youtube-api-token (first args)
          _                 (print "Loading IST titles...")
          ist-titles        (pi/animate! :opts {:frames (:clocks pi/styles)} (load-ist-titles youtube-api-token))
          _                 (println)
          bonus-titles      (load-bonus-titles)
          _                 (println (count ist-titles) "unique IST titles, and" (count bonus-titles) "bonus titles loaded")
          chain             (gen-chain (into ist-titles bonus-titles))]
      (println "Writing Markov chain to" (str chain-filename "..."))
      (with-open [w (io/writer (io/file chain-filename))]
        (pp/pprint chain w)))

    (println "Done.")
    (catch Exception e
      (u/exit -1 (with-out-str (st/print-stack-trace e))))
    (finally
      (u/exit))))
