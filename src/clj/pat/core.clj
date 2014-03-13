
;;;; Patient Waiting Time Simulator
;;
;;  :reload is required otherwise gui goes bad on repeated executions
;;  of core.clj in repl
;;
;;  Modification History
;;  Date        Rel  Who
;;  2013-03-04  0.1  mpw
;;    Initial version

(ns pat
  (:use clojure.repl)
  (:require pat.gui pat.simul :reload))

(def prog-name "Patient Waiting Time Simulator")
(def version "0.1")
(def title (str prog-name " " version))
(def running (atom nil))

(defn stop []
  (when @running
    (let [thread-id @running]
      (reset! running nil)
      (.interrupt thread-id)
      (.join thread-id)
      (gui/set-status "Stopped.")
      (gui/set-title (str title " (stopped)")))))

(defn quit []
  (stop)
  (gui/stop))

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

(defn start []
  (if @running
    (gui/set-status "Already running...")
    (try
      (let [nperiods (gui/get-int :nperiods)
            sla (gui/get-int :sla)
            nreferrals  (gui/get-int :nreferrals)
            nqueued  (gui/get-int :nqueued)
            ndist (gui/get-rbutton :normal)
            nslots (simul/gen-periods [] @gui/slot-vec)
            qtotal (if (zero? nqueued) 0 (+ (* sla nreferrals) nqueued))
            arfn (if ndist #(simul/grand nreferrals) (constantly nreferrals))
            q (simul/mkqueue)]
        (gui/do-edt (gui/clear))
        (gui/do-edt (gui/set-status "Starting simulation..."))
        ;; add queued requests
        (doseq [[p n] (find-span nqueued arfn 1 [])]
          (simul/add-requests q p n))
        ;; run the simulation
        (let [
              result (simul/run-simul q nperiods nslots arfn)
              ;; TBD average wait time
              nbreeched (apply +  (map
                                   #(count (filter (fn [e] (> e sla)) %))
                                   result))
              avq (avqtime result sla)]
          (gui/do-edt (gui/draw-graph result))
          (gui/do-edt
           (gui/messagebox
            "Results"
            (format "Total of %d patients breeched\nAverage queue time: %d"
                    nbreeched (int avq)))))
        (gui/do-edt (gui/set-status "Simulation complete")))
      (catch NumberFormatException _
        (gui/do-edt (gui/messagebox "ERROR" "One or more bad parameters."))))))

;; passed to gui as mouse area callback, so runs in edt
(defn set-slots
  []
  (let [nslots (simul/gen-periods [] @gui/slot-vec)
        nperiods (gui/get-int :nperiods)
        slot-data (take nperiods (map #(repeat % -1) nslots))]
    (gui/draw-graph slot-data)))

(defn main [nperiods sla]
  (gui/do-edt (gui/start title))
  ;; set defaults
  (doseq [[key value] [[:nperiods "52"] [:nreferrals "22"] [:nqueued "0"]
                       [:nslots "40"] [:sla "10"]]]
    (gui/do-edt (gui/set-field key value)))
  ;; callback for mouse action refresh
  (gui/set-mouse-refresh set-slots)
  ;; button actions
  (gui/do-edt (gui/set-callback :run (fn [] (.start (Thread. start)))))
  (gui/do-edt (gui/set-callback :draw set-slots))
  (gui/do-edt (gui/set-callback :clear gui/clear))
  ;; quit needs to run in its own thread, not the EDT.
  (gui/do-edt (gui/set-callback :quit (fn [] (.start (Thread. #(quit)))))))

(defn parse-command-line
  ([args] (parse-command-line args {} nil))
  ([args cmap arg]
     (cond (not (seq args)) cmap
           (.startsWith (first args) "-")
           (recur (rest args) cmap (first args))
           :else (recur (rest args) (conj cmap {arg (first args)}) nil))))
 
(let [cmap (parse-command-line *command-line-args*)
      nperiods (get cmap "-n")
      sla (get cmap "-s")]
  (main nperiods sla))
  
  
