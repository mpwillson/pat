(ns pat
  (:require [goog.net.cookies :as cks]
            [cljs.reader :as reader])
  (:use [clojure.string :only [split]]))

(def prog-name "Patient Waiting Time Simulator")
(def version "0.2")
(def title (str prog-name " " version))

(def unsupported 
  (str "<h1>Unsupported Browser</h1>" 
       "<p>Please consider upgrading to the latest version of Chrome, Firefox, "
       "Opera, Safari or IE9.</p>"))

(def no-cookies 
  (str "<p>Your browser environment prohibits cookies; parameters cannot "
       "be preserved between visits.</p>"))

;; param values are strings to match cookie storage
(def param-keys [:nperiods :nreferrals :ncurrbreach :nslots :sla :navslots 
                 :slots :normaldist])
(def param-defaults ["52" "22" "0" "30" "10" "22" "[[0 22]]" "false"])

(defn set-cookies
  [param-map]
  (let [expires (* 365 3600 24)]
    (doseq [[k v] param-map] (.set goog.net.cookies (name k) v expires))))

(defn get-cookies []
  (let [keys (.getKeys goog.net.cookies)
        vals (.getValues goog.net.cookies)]
    (zipmap (map keyword keys) vals)))
  
(defn find-span
  "Return vector of [period nbooked] in increasing period order."
  [nqueued arfn span booked]
  (if (not (pos? nqueued)) ; true for 0 or negative
    (reverse booked)
    (let [nbookings (arfn)
          newq (- nqueued nbookings)
          nq (if (neg? newq) nqueued nbookings)]
      (recur newq  arfn (inc span) (conj booked [(- span) nq])))))

(defn rolling-average
  [av lst sla]
  (let [missed (->> lst (filter pos?) (filter #(> % sla)))
        n (count missed)
        newav (if (zero? n) av (/ (reduce + missed) n))]
    (if (zero? av)
      newav
      (/ (+ av newav) 2))))

(defn avqtime
  [result sla]
  (reduce #(rolling-average %1 %2 sla) 0 result))

(defn get-empty-count
  [lst]
  (reduce + (for [p lst] (count (filter neg? p)))))

(defn get-params 
  "Return map of param keys and values from current gui settings."
  [keys]
  (reduce #(assoc %1 %2 (gui/get-value %2)) {} keys))

(defn start []
  (try
      (let [p (get-params param-keys)
            ndist (gui/get-checked :normal)
            period-slots (simul/gen-periods [] @gui/slot-vec)
            qtotal (if (zero? (:ncurrbreach p))
                     0 
                     (+ (* (:sla p) (:nreferrals p)) (:ncurrbreach p)))
            arfn (if ndist 
                   #(simul/grand (:nreferrals p)) 
                   (constantly (:nreferrals p)))
            q (simul/mkqueue)]
        (gui/clear)
        (set-cookies (assoc p :slots @gui/slot-vec :normaldist ndist))
        ;; add queued requests (always assume uniform arrival rate)
        (doseq [[p n] (find-span qtotal (constantly (:nreferrals p)) 1 [])]
          (simul/add-requests q p n))
        ;; run the simulation
        (let [result (simul/run-simul q (:nperiods p) period-slots arfn)
              nbreached (apply + (map
                                  #(count (filter (fn [e] (> e (:sla p))) %))
                                  result))
              avq (avqtime result (:sla p))
              unusedslots (get-empty-count result)]
          (gui/draw-graph result)
          (doseq [[k v] {:nbreached (str nbreached) 
                         :avqtime (format "%.1f" avq)
                         :unusedslots (str unusedslots)}]
            (gui/set-value k v))))
      (catch js/Error _
        (js/alert (str "ERROR: " _)))))

(defn set-slots
  [initialise]
  (when initialise (gui/init-slots [[0 (gui/get-value :navslots)]]))
  (let [period-slots (simul/gen-periods [] @gui/slot-vec)
        nperiods (gui/get-value :nperiods)
        slot-data (take nperiods (map #(repeat % -1) period-slots))]
    (gui/draw-graph slot-data)))

(defn import-file [text]
  (js/alert text))

(defn setup []
  (if (not (gui/canvas-available))
    (.write js/document unsupported)
    (let [param-map  (zipmap param-keys param-defaults)]
      (gui/set-value :version (str "Version " version))
      (when (.isEmpty goog.net.cookies)
        (set-cookies param-map))
      (let [cookies (get-cookies)
            params (or (not-empty cookies) param-map)]
        (when (empty? cookies)
          (.insertAdjacentHTML (.-body js/document) "beforeEnd" no-cookies))
        (doseq [[k v] params] (gui/set-value k v))
        (gui/init-slots (reader/read-string (:slots params)))
        (gui/set-checked :normal (reader/read-string (:normaldist params)))
        (gui/set-file-listener import-file)
        (gui/set-mouse-refresh set-slots)))))
