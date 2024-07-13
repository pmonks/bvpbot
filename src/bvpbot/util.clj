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

(ns bvpbot.util
  (:require [clojure.string         :as s]
            [clojure.tools.logging  :as log]
            [cheshire.core          :as ch]
            [camel-snake-kebab.core :as csk]
            [java-time              :as tm]))

(def boot-time (tm/instant))

(defn nth-fibonacci
  "Returns the nth fibonacci."
  [n]
  (loop [series [0 1]]
    (if (> (count series) n)
      (nth series n)
      (let [[n-1 n-2] (rseq series)]  ; We use rseq here as it is constant time on vectors (vs reverse, which is linear time)
        (recur (conj series (+' n-1 n-2)))))))

(defn parse-int
  "Parses a value (a string or numeric) into a Clojure integer (Java Long or BigInteger), returning nil if parsing failed."
  [x]
  (cond (integer?  x) x
        (string?   x) (try
                        (Long/parseLong (s/trim x))
                        (catch NumberFormatException _
                          nil))
        (float?    x) (int (Math/round ^Float x))
        (double?   x) (int (Math/round ^Double x))
        (rational? x) (parse-int (double x))
        :else         nil))

(defn safe-parse-boolean
  "Because clojure.core/boolean is not nil tolerant 🙄"
  [x]
  (when x
    (parse-boolean x)))

(defn parse-json
  "Parse a JSON string into Clojure data structures."
  [^String json]
  (when-not (s/blank? json)
    (ch/parse-string json csk/->kebab-case-keyword)))

(defn getrn
  "Like get, but also replace nil values found in the map with the default value."
  [m k nf]
  (or (get m k nf) nf))

(defn mapfonk
  "Returns a new map where f has been applied to all of the keys of m."
  [f m]
  (when m
    (into {}
          (for [[k v] m]
            [(f k) v]))))

(defn mapfonv
  "Returns a new map where f has been applied to all of the values of m."
  [f m]
  (when m
    (into {}
          (for [[k v] m]
            [k (f v)]))))

(defn is-url?
  "Is s a valid URL?"
  [^String s]
  ; Note: UrlValidator.isValid() is null-safe (returns false when given null), so we don't need to guard that ourselves
  (.isValid (org.apache.commons.validator.routines.UrlValidator/getInstance) s))

(defn replace-all
  "Takes a sequence of replacements, and applies all of them to the given string, in the order provided.  Each replacement in the sequence is a pair of values to be passed to clojure.string/replace (the 2nd and 3rd arguments)."
  [string replacements]
  (when (and string (seq replacements))
    (loop [s string
           f (first replacements)
           r (rest  replacements)]
      (if f
        (recur (s/replace s (first f) (second f))
               (first r)
               (rest  r))
        s))))

(defn to-ascii
  "Converts the given string to ASCII, mapping a small number of Unicode characters to their ASCII equivalents."
  [s]
  (replace-all s
               [[#"\p{javaWhitespace}" " "]     ; Whitespace
                [#"[–‑‒–—]"            "-"]     ; Hyphens / dashes
                [#"[^\p{ASCII}]+"      ""]]))   ; Strip everything else

(defn truncate
  "If s is longer than len, truncates it to len-1 and adds the ellipsis (…) character to the end."
  [s len]
  (if (> (count s) len)
    (str (s/trim (subs s 0 (dec len))) "…")
    s))

(defmacro in-tz
  "Executes body (assumed to include java-time logic) within the given tzdata timezone (e.g. Americas/Los_Angeles)."
  [tz & body]
  `(tm/with-clock (tm/system-clock ~tz) ~@body))

(defn human-readable-date-diff
  "Returns a human readable String containing the human readable difference between two instants."
  [^java.time.Instant i1
   ^java.time.Instant i2]
  (format "%dd %dh %dm %d.%03ds" (.until i1 i2 (java.time.temporal.ChronoUnit/DAYS))
                                 (mod (.until i1 i2 (java.time.temporal.ChronoUnit/HOURS))     24)
                                 (mod (.until i1 i2 (java.time.temporal.ChronoUnit/MINUTES))   60)
                                 (mod (.until i1 i2 (java.time.temporal.ChronoUnit/SECONDS))   60)
                                 (mod (.until i1 i2 (java.time.temporal.ChronoUnit/MILLIS))  1000)))

(def ^:private units ["B" "KB" "MB" "GB" "TB" "PB" "EB" "ZB" "YB"])
(def ^:private ^java.text.DecimalFormat df (java.text.DecimalFormat. "#.##"))

(defn human-readable-size
  "Returns a human readable String for the given computer size (e.g. 2GB)."
  [size]
  (let [index (loop [size size
                     index 0]
                (if (< size 1024)
                  index
                  (recur (/ size 1024) (inc index))))]
    (str (.format df (/ size (Math/pow 1024 index))) (nth units index))))

(defn- unwrap-to-exinfo
  "Unwraps the given throwable to the first ExceptionInfo instance, or returns nil."
  [^java.lang.Throwable e]
  (when e
    (if (instance? clojure.lang.ExceptionInfo e)
      e
      (recur (.getCause e)))))

(defn log-exception
  "Logs the given exception and (optional) message at ERROR level."
  ([^java.lang.Throwable e] (log-exception e nil))
  ([^java.lang.Throwable e msg]
   (let [ei    (unwrap-to-exinfo e)
         extra (ex-data ei)
         m     (case [(s/blank? msg) (boolean extra)]
                 [false  true]  (str msg "; data: " extra)
                 [false  false] msg
                 [true true]  (str "Data: " extra)
                 [true false] (if e (.getMessage e) "No exception information provided (this is probably a bug)"))]
     (log/error e m))))

(defn exit
  "Exits the program after printing the given message, and returns the given status code."
  ([]            (exit 0 nil))
  ([status-code] (exit status-code nil))
  ([status-code message]
   (when-not (s/blank? message)
     (if (= 0 status-code)
       (println message)
       (binding [*out* *err*]
         (println message)))
     (flush))
   (shutdown-agents)
   (System/exit status-code)))

; Various sys info strings
(def clojure-info (str "v" (clojure-version)))
(def jvm-info     (str (System/getProperty "java.version") " (" (System/getProperty "java.vm.name") " " (System/getProperty "java.vm.version") ")"))
(def os-info      (str (System/getProperty "os.name") " " (System/getProperty "os.version") " " (System/getProperty "os.arch") " (" (.availableProcessors (Runtime/getRuntime)) " cores)"))

(def ^:private ^java.lang.management.MemoryMXBean mem-mx-bean (java.lang.management.ManagementFactory/getMemoryMXBean))

(defn- mem-info-str
  [maximum used]
  (format "%s used of %s%s"
          (human-readable-size used)
          (if (pos? maximum)
            (str (human-readable-size maximum) " max")
            "[unlimited]")
          (if (pos? maximum)
            (format " (%.2f%%)" (/ used maximum 0.01))
            "")))

(defn heap-mem-info
  "Heap memory info, as a human-readable String."
  []
  (let [heap-usage (.getHeapMemoryUsage mem-mx-bean)
        heap-max   (.getMax  heap-usage)
        heap-used  (.getUsed heap-usage)]
    (mem-info-str heap-max heap-used)))

(defn non-heap-mem-info
  "Non-heap memory info, as a human-readable String."
  []
  (let [non-heap-usage (.getNonHeapMemoryUsage mem-mx-bean)
        non-heap-max   (.getMax  non-heap-usage)
        non-heap-used  (.getUsed non-heap-usage)]
    (mem-info-str non-heap-max non-heap-used)))

(defn runtime-info
  "How long the bot has been running, as a human-readable String."
  []
  (human-readable-date-diff boot-time (tm/instant)))
