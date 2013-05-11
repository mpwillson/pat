(ns pat)

(def prog-name "Patient Waiting Time Simulator")
(def version "0.1")
(def title (str prog-name " " version))

(defn find-span
  "Return vector of [period nbooked] in increasing period order."
  [nqueued arfn span booked]
  (if (not (pos? nqueued))
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

(defn start []
  (try
      (let [nperiods (gui/get-int :nperiods)
            sla (gui/get-int :sla)
            nreferrals  (gui/get-int :nreferrals)
            nqueued  (gui/get-int :nqueued)
            ndist (gui/get-checked :normal)
            nslots (simul/gen-periods [] @gui/slot-vec)
            qtotal (if (zero? nqueued) 0 (+ (* sla nreferrals) nqueued))
            arfn (if ndist #(simul/grand nreferrals) (constantly nreferrals))
            q (simul/mkqueue)]
        (gui/clear)
        ;; add queued requests
        (doseq [[p n] (find-span nqueued arfn 1 [])]
          (simul/add-requests q p n))
        ;; run the simulation
        (let [result (simul/run-simul q nperiods nslots arfn)
              nbreached (apply +  (map
                                   #(count (filter (fn [e] (> e sla)) %))
                                   result))
              avq (avqtime result sla)
              unusedslots (get-empty-count result)]
          (gui/draw-graph result)
          (gui/set-value :nbreached (str nbreached))
          (gui/set-value :avqtime (format "%.1f" avq))
          (gui/set-value :unusedslots (str unusedslots))))
      (catch js/Error _
        (js/alert (str "ERROR: " _)))))

(defn set-slots
  [initialise]
  (when initialise (gui/init-slots [[0 (gui/get-int :navslots)]]))
  (let [nslots (simul/gen-periods [] @gui/slot-vec)
        nperiods (gui/get-int :nperiods)
        slot-data (take nperiods (map #(repeat % -1) nslots))]
    (gui/draw-graph slot-data)))


(defn setup []
    (doseq [[key value] [[:nperiods "52"] [:nreferrals "22"] [:nqueued "0"]
                         [:nslots "30"] [:sla "10"] [:navslots "22"]]]
      (gui/set-value key value))
    (gui/init-slots [[0 (gui/get-int :navslots)]])
    (if (gui/canvas-available)
      (gui/set-mouse-refresh set-slots)
      (.write js/document "<h1>Unsupported Browser</h1> <p>Please consider upgrading to the latest version of Chrome, Firefox, Opera, Safari or IE.</p>")))
;; TBD 
;;
;; Determine if browser is supported
